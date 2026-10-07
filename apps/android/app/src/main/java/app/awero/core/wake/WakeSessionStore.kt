package app.awero.core.wake

import android.content.Context
import app.awero.core.alarm.Alarm
import app.awero.core.storage.AweroDatabase
import app.awero.core.storage.WakeSessionEntity
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

    suspend fun start(alarm: Alarm, scheduledAt: Long = System.currentTimeMillis()): WakeSession {
        migrateLegacyIfNeeded()
        val existing = database.wakeSessions().active()
        if (existing != null && existing.alarmId == alarm.id && existing.alarmVersion == alarm.version) {
            active = WakeSessionMapper.fromEntity(existing)
            return active!!
        }

        val session = WakeSession(
            id = UUID.randomUUID().toString(),
            alarmId = alarm.id,
            alarmVersion = alarm.version,
            scheduledAt = scheduledAt
        )
        active = session
        save(session)
        return session
    }

    suspend fun startMission() {
        active?.let {
            it.triggeredAt = it.triggeredAt ?: System.currentTimeMillis()
            it.missionStartedAt = System.currentTimeMillis()
            save(it)
        }
    }

    suspend fun markFallback() {
        active?.let {
            it.fallbackUsed = true
            save(it)
        }
    }

    suspend fun complete(): WakeSession? {
        val session = active ?: return null
        session.completedAt = System.currentTimeMillis()
        session.result = "SUCCESS"
        save(session)
        active = null
        return session
    }

    suspend fun setSnoozeCount(count: Int) {
        active?.let {
            it.snoozeCount = count
            save(it)
        }
    }

    suspend fun emergencyStop(): WakeSession? {
        val session = active ?: return null
        session.emergencyStop = true
        session.result = "EMERGENCY_STOP"
        session.completedAt = System.currentTimeMillis()
        save(session)
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

    private suspend fun migrateLegacyIfNeeded() {
        if (preferences.getBoolean("room_migrated", false)) return
        migrationMutex.withLock {
            if (preferences.getBoolean("room_migrated", false)) return
            val raw = preferences.getString("sessions", null)
            if (!raw.isNullOrBlank()) {
                runCatching {
                    val array = JSONArray(raw)
                    for (i in 0 until array.length()) {
                        database.wakeSessions().upsert(
                            WakeSessionMapper.toEntity(fromJson(array.getJSONObject(i)))
                        )
                    }
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
        emergencyStop = o.optBoolean("emergencyStop", false)
    )

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (isNull(key)) null else optLong(key)

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key)
}
