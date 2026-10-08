package app.awero.core.alarm

import android.content.Context
import app.awero.core.storage.AlarmMapper
import app.awero.core.storage.AweroDatabase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

class AlarmStore(
    context: Context,
    private val database: AweroDatabase = AweroDatabase.get(context.applicationContext)
) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("awero_alarms", Context.MODE_PRIVATE)
    private val migrationMutex = Mutex()

    suspend fun save(alarm: Alarm) {
        migrateLegacyIfNeeded()
        database.alarms().upsert(AlarmMapper.toEntity(alarm))
    }

    suspend fun get(id: String): Alarm? {
        migrateLegacyIfNeeded()
        return database.alarms().get(id)?.let(AlarmMapper::fromEntity)
    }

    suspend fun all(): List<Alarm> {
        migrateLegacyIfNeeded()
        return database.alarms().all().map(AlarmMapper::fromEntity)
    }

    suspend fun delete(id: String) {
        migrateLegacyIfNeeded()
        database.alarms().get(id)?.let { database.alarms().delete(it) }
    }

    private suspend fun migrateLegacyIfNeeded() {
        if (preferences.getBoolean("room_migrated", false)) return
        migrationMutex.withLock {
            if (preferences.getBoolean("room_migrated", false)) return
            val ids = preferences.getStringSet("ids", emptySet()).orEmpty()
            for (id in ids) {
                val raw = preferences.getString("alarm:$id", null) ?: continue
                val migrated = runCatching {
                    database.alarms().upsert(AlarmMapper.toEntity(fromJson(JSONObject(raw))))
                }
                if (migrated.isFailure) return
            }
            preferences.edit().putBoolean("room_migrated", true).apply()
        }
    }

    private fun fromJson(o: JSONObject): Alarm {
        val days = buildSet {
            val x = o.getJSONArray("weekdays")
            for (i in 0 until x.length()) add(x.getInt(i))
        }
        return Alarm(
            o.getString("id"),
            o.getInt("version"),
            o.getInt("hour"),
            o.getInt("minute"),
            o.getBoolean("enabled"),
            days,
            TimezoneMode.valueOf(o.getString("timezoneMode")),
            o.optString("fixedTimezone", null),
            MissionType.valueOf(o.getString("missionType")),
            Difficulty.valueOf(o.getString("difficulty")),
            o.optInt("maxSnoozes", 3),
            o.optInt("snoozeMinutes", 10),
            o.optString("qrExpectedCode", null)
        )
    }
}
