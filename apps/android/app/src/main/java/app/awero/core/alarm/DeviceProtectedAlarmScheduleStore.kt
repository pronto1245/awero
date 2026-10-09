package app.awero.core.alarm

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Minimal alarm schedule projection available before the first device unlock after reboot. */
internal class DeviceProtectedAlarmScheduleStore(context: Context) {
    private val preferences = context.applicationContext
        .createDeviceProtectedStorageContext()
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    suspend fun upsert(alarm: AlarmSchedule): Boolean = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val ids = preferences.getStringSet(KEY_IDS, emptySet()).orEmpty().toMutableSet()
            val editor = preferences.edit()
            if (alarm.enabled) {
                ids += alarm.id
                editor.putString(alarmKey(alarm.id), toJson(alarm).toString())
            } else {
                ids -= alarm.id
                editor.remove(alarmKey(alarm.id))
            }
            editor.putStringSet(KEY_IDS, ids).commit()
        }
    }

    suspend fun remove(id: String): Boolean = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val ids = preferences.getStringSet(KEY_IDS, emptySet()).orEmpty().toMutableSet()
            ids -= id
            preferences.edit()
                .putStringSet(KEY_IDS, ids)
                .remove(alarmKey(id))
                .commit()
        }
    }

    suspend fun replaceAll(alarms: List<AlarmSchedule>): Boolean = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val enabledAlarms = alarms.filter(AlarmSchedule::enabled)
            val nextIds = enabledAlarms.mapTo(mutableSetOf(), AlarmSchedule::id)
            val previousIds = preferences.getStringSet(KEY_IDS, emptySet()).orEmpty()
            val editor = preferences.edit().putStringSet(KEY_IDS, nextIds)
            (previousIds - nextIds).forEach { editor.remove(alarmKey(it)) }
            enabledAlarms.forEach { editor.putString(alarmKey(it.id), toJson(it).toString()) }
            editor.commit()
        }
    }

    suspend fun get(id: String): AlarmSchedule? = withContext(Dispatchers.IO) {
        synchronized(lock) {
            if (id !in preferences.getStringSet(KEY_IDS, emptySet()).orEmpty()) return@synchronized null
            val raw = preferences.getString(alarmKey(id), null) ?: return@synchronized null
            runCatching { fromJson(JSONObject(raw)) }
                .onFailure { Log.e(TAG, "Could not read device-protected alarm schedule: $id", it) }
                .getOrNull()
        }
    }

    suspend fun all(): List<AlarmSchedule> = withContext(Dispatchers.IO) {
        synchronized(lock) {
            preferences.getStringSet(KEY_IDS, emptySet()).orEmpty().mapNotNull { id ->
                val raw = preferences.getString(alarmKey(id), null) ?: return@mapNotNull null
                runCatching { fromJson(JSONObject(raw)) }
                    .onFailure { Log.e(TAG, "Could not read device-protected alarm schedule: $id", it) }
                    .getOrNull()
            }
        }
    }

    private fun toJson(alarm: AlarmSchedule) = JSONObject()
        .put("id", alarm.id)
        .put("version", alarm.version)
        .put("hour", alarm.hour)
        .put("minute", alarm.minute)
        .put("weekdays", JSONArray(alarm.weekdays.sorted()))
        .put("timezoneMode", alarm.timezoneMode.name)
        .put("fixedTimezone", alarm.fixedTimezone ?: JSONObject.NULL)

    private fun fromJson(json: JSONObject): AlarmSchedule {
        val weekdays = buildSet {
            val array = json.getJSONArray("weekdays")
            for (index in 0 until array.length()) add(array.getInt(index))
        }
        return AlarmSchedule(
            id = json.getString("id"),
            version = json.getInt("version"),
            hour = json.getInt("hour"),
            minute = json.getInt("minute"),
            enabled = true,
            weekdays = weekdays,
            timezoneMode = TimezoneMode.valueOf(json.getString("timezoneMode")),
            fixedTimezone = if (json.isNull("fixedTimezone")) null else json.getString("fixedTimezone")
        )
    }

    private fun alarmKey(id: String) = "$KEY_ALARM_PREFIX$id"

    private companion object {
        const val TAG = "AWERO.DirectBoot"
        const val PREFERENCES_NAME = "awero_direct_boot_alarm_schedules"
        const val KEY_IDS = "ids"
        const val KEY_ALARM_PREFIX = "alarm:"
        val lock = Any()
    }
}
