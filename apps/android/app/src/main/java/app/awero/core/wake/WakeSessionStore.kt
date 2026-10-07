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

class WakeSessionStore(context: Context) {
    private val appContext = context.applicationContext
    private val database = AweroDatabase.get(appContext)
    private val preferences = appContext.getSharedPreferences("awero_wake_sessions", Context.MODE_PRIVATE)
    private val migrationMutex = Mutex()
    private var active: WakeSession? = null

    suspend fun start(alarm: Alarm, scheduledAt: Long = System.currentTimeMillis()): Boolean {
        migrateLegacyIfNeeded()
        val existing = database.wakeSessions().active()
        if (existing != null && existing.alarmId == alarm.id && existing.alarmVersion == alarm.version) {
            active = WakeSessionMapper.fromEntity(existing)
            return false
        }

        val session = WakeSession(
            id = UUID.randomUUID().toString(),
            alarmId = alarm.id,
            alarmVersion = alarm.version,
            scheduledAt = scheduledAt,
            triggeredAt = System.currentTimeMillis()
        )
        active = session
        saveWithEvent(session, "TRIGGERED")
        return true
    }

    suspend fun startMission() {
        ensureActive()
        active?.let {
            it.triggeredAt = it.triggeredAt ?: System.currentTimeMillis()
            it.missionStartedAt = System.currentTimeMillis()
            saveWithEvent(it, "MISSION_STARTED")
        }
    }

    suspend fun markFallback() {
        ensureActive()
        active?.let {
            it.fallbackUsed = true
            saveWithEvent(it, "FALLBACK")
        }
    }

    suspend fun complete(): WakeSession? {
        ensureActive()
        val session = active ?: return null
        if (session.result != null) return session
        session.completedAt = System.currentTimeMillis()
        session.result = "SUCCESS"
        saveWithEvent(session, "COMPLETED")
        active = null
        return session
    }

    suspend fun setSnoozeCount(count: Int) {
        ensureActive()
        active?.let {
            it.snoozeCount = count
            saveWithEvent(it, "SNOOZE", """{"count":$count}""")
        }
    }

    suspend fun emergencyStop(): WakeSession? {
        ensureActive()
        val session = active ?: return null
        if (session.result != null) return session
        session.emergencyStop = true
        session.result = "EMERGENCY_STOP"
        session.completedAt = System.currentTimeMillis()
        saveWithEvent(session, "EMERGENCY_STOP")
        active = null
        return session
    }

    suspend fun save(session: WakeSession) {
        migrateLegacyIfNeeded()
        database.wakeSessions().upsert(WakeSessionMapper.toEntity(session))
    }

    suspend fun load(): List<WakeSession> {
        migrateLegacyIfNeeded()
        return database.wakeSessions().recent().map(WakeSessionMapper::fromEntity)
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
        if (active == null) {
            active = database.wakeSessions().active()?.let(WakeSessionMapper::fromEntity)
        }
    }

    private suspend fun migrateLegacyIfNeeded() {
        if (preferences.getBoolean("room_migrated", false)) return
        migrationMutex.withLock {
            if (preferences.getBoolean("room_migrated", false)) return
            val raw = preferences.getString("sessions", null)
            if (!raw.isNullOrBlank()) {
                val migrated = runCatching {
                    val array = JSONArray(raw)
                    for (i in 0 until array.length()) {
                        database.wakeSessions().upsert(
                            WakeSessionMapper.toEntity(fromJson(array.getJSONObject(i)))
                        )
                    }
                }
                if (migrated.isFailure) return
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
        emergencyStop = o.optBoolean("emergencyStop", false)
    )

    private fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key)) null else optLong(key)
    private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else optString(key)
}