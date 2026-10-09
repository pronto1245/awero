package app.awero.core.alarm

import android.content.Context
import app.awero.core.storage.AlarmMapper
import app.awero.core.storage.AweroDatabase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

class AlarmStore private constructor(
    context: Context,
    private val database: AweroDatabase,
    private val deviceProtectedSchedules: DeviceProtectedAlarmScheduleStore?
) {
    constructor(context: Context) : this(
        context.applicationContext,
        AweroDatabase.get(context.applicationContext),
        DeviceProtectedAlarmScheduleStore(context.applicationContext)
    )

    constructor(context: Context, database: AweroDatabase) : this(context, database, null)

    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("awero_alarms", Context.MODE_PRIVATE)
    private val migrationMutex = Mutex()

    suspend fun save(alarm: Alarm) {
        migrateLegacyIfNeeded()
        val previous = database.alarms().get(alarm.id)?.let(AlarmMapper::fromEntity)
        database.alarms().upsert(AlarmMapper.toEntity(alarm))
        try {
            check(deviceProtectedSchedules?.upsert(alarm.toSchedule()) != false) {
                "Could not update the direct-boot alarm schedule."
            }
        } catch (error: Exception) {
            restore(alarm.id, previous)
            runCatching {
                if (previous == null) deviceProtectedSchedules?.remove(alarm.id)
                else deviceProtectedSchedules?.upsert(previous.toSchedule())
            }
            throw error
        }
    }

    suspend fun get(id: String): Alarm? {
        migrateLegacyIfNeeded()
        return database.alarms().get(id)?.let(AlarmMapper::fromEntity)
    }

    suspend fun all(): List<Alarm> {
        migrateLegacyIfNeeded()
        val alarms = database.alarms().all().map(AlarmMapper::fromEntity)
        check(deviceProtectedSchedules?.replaceAll(alarms.map(Alarm::toSchedule)) != false) {
            "Could not reconcile the direct-boot alarm schedules."
        }
        return alarms
    }

    suspend fun delete(id: String) {
        migrateLegacyIfNeeded()
        val previous = database.alarms().get(id)?.let(AlarmMapper::fromEntity) ?: return
        database.alarms().delete(AlarmMapper.toEntity(previous))
        try {
            check(deviceProtectedSchedules?.remove(id) != false) {
                "Could not remove the direct-boot alarm schedule."
            }
        } catch (error: Exception) {
            restore(id, previous)
            runCatching { deviceProtectedSchedules?.upsert(previous.toSchedule()) }
            throw error
        }
    }

    private suspend fun restore(id: String, previous: Alarm?) {
        if (previous == null) {
            database.alarms().get(id)?.let { database.alarms().delete(it) }
        } else {
            database.alarms().upsert(AlarmMapper.toEntity(previous))
        }
    }

    private suspend fun migrateLegacyIfNeeded() {
        if (preferences.getBoolean("room_migrated", false)) return
        migrationMutex.withLock {
            if (preferences.getBoolean("room_migrated", false)) return
            val ids = preferences.getStringSet("ids", emptySet()).orEmpty()
            for (id in ids) {
                if (database.alarms().get(id) != null) continue
                val raw = preferences.getString("alarm:$id", null)
                    ?: error("Legacy alarm $id is missing; migration was not marked complete.")
                database.alarms().upsert(AlarmMapper.toEntity(fromJson(JSONObject(raw))))
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
