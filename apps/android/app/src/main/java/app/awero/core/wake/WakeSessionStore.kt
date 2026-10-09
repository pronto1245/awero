package app.awero.core.wake

import android.content.Context
import app.awero.core.alarm.Alarm
import app.awero.core.storage.AweroDatabase
import app.awero.core.storage.WakeEventEntity
import app.awero.core.storage.WakeSessionMapper
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class WakeSessionStore(
    context: Context,
    private val database: AweroDatabase = AweroDatabase.get(context.applicationContext)
) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("awero_wake_sessions", Context.MODE_PRIVATE)
    private val migrationMutex = Mutex()
    private var active: WakeSession? = null

    suspend fun start(
        alarm: Alarm,
        scheduledAt: Long = System.currentTimeMillis(),
        isTest: Boolean = false
    ): Boolean = transitionMutex.withLock {
        migrateLegacyIfNeeded()
        val existing = database.wakeSessions().activeForAlarm(alarm.id, isTest)
        if (existing != null && existing.alarmVersion == alarm.version) {
            active = WakeSessionMapper.fromEntity(existing)
            return false
        }

        val session = WakeSession(
            id = UUID.randomUUID().toString(),
            alarmId = alarm.id,
            alarmVersion = alarm.version,
            scheduledAt = scheduledAt,
            triggeredAt = System.currentTimeMillis(),
            isTest = isTest
        )
        val activeForAlarm = existing
        if (activeForAlarm == null) {
            saveWithEvent(session, "TRIGGERED")
        } else {
            // A changed alarm version leaves the prior event history intact and closes its stale active row.
            val interrupted = WakeSessionMapper.fromEntity(activeForAlarm).copy(
                result = "INTERRUPTED",
                completedAt = System.currentTimeMillis()
            )
            saveWithEvent(interrupted, "INTERRUPTED")
            saveWithEvent(session, "TRIGGERED")
        }
        active = session
        true
    }

    suspend fun startMission(): Boolean = transitionMutex.withLock {
        ensureActive()
        val session = active ?: return false
        if (session.result != null || session.missionStartedAt != null) return false
        val updated = session.copy(
            triggeredAt = session.triggeredAt ?: System.currentTimeMillis(),
            missionStartedAt = System.currentTimeMillis()
        )
        saveWithEvent(updated, "MISSION_STARTED")
        active = updated
        true
    }

    suspend fun markFallback(): Boolean = transitionMutex.withLock {
        ensureActive()
        val session = active ?: return false
        if (session.result != null || session.fallbackUsed) return false
        val updated = session.copy(fallbackUsed = true)
        saveWithEvent(updated, "FALLBACK")
        active = updated
        true
    }

    suspend fun complete(): WakeSession? = transitionMutex.withLock {
        ensureActive()
        val session = active ?: return null
        if (session.result != null) return null
        val updated = session.copy(
            completedAt = System.currentTimeMillis(),
            result = "SUCCESS"
        )
        saveWithEvent(updated, "COMPLETED")
        active = null
        updated
    }

    suspend fun setSnoozeCount(count: Int): Boolean = transitionMutex.withLock {
        ensureActive()
        val session = active ?: return false
        if (session.result != null || count == session.snoozeCount) return false
        val updated = session.copy(snoozeCount = count)
        saveWithEvent(updated, "SNOOZE", """{"count":$count}""")
        active = updated
        true
    }

    suspend fun emergencyStop(): WakeSession? = transitionMutex.withLock {
        ensureActive()
        val session = active ?: return null
        if (session.result != null) return null
        val updated = session.copy(
            emergencyStop = true,
            result = "EMERGENCY_STOP",
            completedAt = System.currentTimeMillis()
        )
        saveWithEvent(updated, "EMERGENCY_STOP")
        active = null
        updated
    }

    suspend fun save(session: WakeSession) {
        transitionMutex.withLock {
            migrateLegacyIfNeeded()
            database.wakeSessions().upsert(WakeSessionMapper.toEntity(session))
            if (active?.id == session.id) active = session
        }
    }

    suspend fun loadActive(alarmId: String? = null, isTest: Boolean? = null): WakeSession? =
        transitionMutex.withLock {
            migrateLegacyIfNeeded()
            val entity = if (alarmId != null && isTest != null) {
                database.wakeSessions().activeForAlarm(alarmId, isTest)
            } else {
                database.wakeSessions().active()
            }
            entity?.let(WakeSessionMapper::fromEntity).also { active = it }
        }

    suspend fun load(includeTestAlarms: Boolean = false): List<WakeSession> {
        return transitionMutex.withLock {
            migrateLegacyIfNeeded()
            database.wakeSessions().recent(includeTestAlarms).map(WakeSessionMapper::fromEntity)
        }
    }

    private suspend fun saveWithEvent(session: WakeSession, type: String, payload: String = "{}") {
        migrateLegacyIfNeeded()
        database.wakeSessions().upsertWithEvent(
            WakeSessionMapper.toEntity(session),
            WakeEventEntity(
                id = UUID.randomUUID().toString(),
                wakeSessionId = session.id,
                eventType = type,
                occurredAt = System.currentTimeMillis(),
                payload = payload
            )
        )
    }

    private suspend fun ensureActive() {
        active = active?.let { database.wakeSessions().byId(it.id)?.let(WakeSessionMapper::fromEntity) }
            ?: database.wakeSessions().active()?.let(WakeSessionMapper::fromEntity)
    }

    private suspend fun migrateLegacyIfNeeded() {
        if (preferences.getBoolean("room_migrated", false)) return
        migrationMutex.withLock {
            if (preferences.getBoolean("room_migrated", false)) return
            val raw = preferences.getString("sessions", null)
            if (!raw.isNullOrBlank()) {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    database.wakeSessions().upsert(
                        WakeSessionMapper.toEntity(fromJson(array.getJSONObject(i)))
                    )
                }
            }
            preferences.edit().putBoolean("room_migrated", true).apply()
        }
    }

    private fun fromJson(o: JSONObject) = WakeSession(
        id = o.getString("id"),
        alarmId = o.getString("alarmId"),
        alarmVersion = o.getInt("alarmVersion"),
        scheduledAt = o.getLong("scheduledAt"),
        triggeredAt = o.optLongOrNull("triggeredAt"),
        missionStartedAt = o.optLongOrNull("missionStartedAt"),
        completedAt = o.optLongOrNull("completedAt"),
        result = o.optStringOrNull("result"),
        snoozeCount = o.optInt("snoozeCount", 0),
        fallbackUsed = o.optBoolean("fallbackUsed", false),
        emergencyStop = o.optBoolean("emergencyStop", false),
        isTest = o.optBoolean("isTest", false)
    )

    private fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key)) null else optLong(key)
    private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else optString(key)

    private companion object {
        val transitionMutex = Mutex()
    }
}
