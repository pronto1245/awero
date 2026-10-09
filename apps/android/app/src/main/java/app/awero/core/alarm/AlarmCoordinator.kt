package app.awero.core.alarm

import android.content.Context
import android.util.Log
import app.awero.core.sync.OfflineSyncCoordinator
import app.awero.core.sync.SyncQueueStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AlarmCoordinator(
    private val context: Context,
    private val store: AlarmStore = AlarmStore(context),
    private val scheduler: AlarmScheduler = AlarmScheduler(context)
) {

    suspend fun all(): List<Alarm> = store.all().sortedWith(compareBy<Alarm> { it.hour }.thenBy { it.minute })

    suspend fun create(
        hour: Int,
        minute: Int,
        missionType: MissionType = MissionType.MATH,
        difficulty: Difficulty = Difficulty.MEDIUM,
        qrExpectedCode: String? = null,
        weekdays: Set<Int> = (1..7).toSet(),
        timezoneMode: TimezoneMode = TimezoneMode.DEVICE_LOCAL,
        fixedTimezone: String? = null,
        label: String = "Alarm"
    ): Alarm {
        val alarm = Alarm(
            id = UUID.randomUUID().toString(),
            version = 1,
            hour = hour,
            minute = minute,
            enabled = true,
            weekdays = weekdays,
            timezoneMode = timezoneMode,
            fixedTimezone = if (timezoneMode == TimezoneMode.FIXED) fixedTimezone else null,
            missionType = missionType,
            difficulty = difficulty,
            qrExpectedCode = qrExpectedCode,
            label = label.trim().take(80).ifBlank { "Alarm" }
        )
        AlarmNotificationManager.requireAlarmAccess(context)
        try {
            store.save(alarm)
            scheduler.schedule(alarm)
            enqueueSync("CREATE_ALARM", alarm, alarm.version)
            return alarm
        } catch (error: Exception) {
            scheduler.cancel(alarm)
            store.delete(alarm.id)
            throw error
        }
    }

    suspend fun update(alarm: Alarm): Alarm {
        val next = alarm.copy(version = alarm.version + 1)
        if (next.enabled) AlarmNotificationManager.requireAlarmAccess(context)
        store.save(next)
        try {
            scheduler.schedule(next)
            enqueueSync("UPDATE_ALARM", next, alarm.version)
            return next
        } catch (error: Exception) {
            store.save(alarm)
            runCatching { scheduler.schedule(alarm) }
            throw error
        }
    }

    suspend fun delete(alarm: Alarm) {
        store.delete(alarm.id)
        scheduler.cancel(alarm)
        enqueueSync("DELETE_ALARM", alarm, alarm.version, JSONObject())
    }

    fun test(alarm: Alarm) {
        AlarmNotificationManager.requireAlarmAccess(context)
        scheduler.scheduleTest(alarm)
    }

    private suspend fun enqueueSync(
        operationType: String,
        alarm: Alarm,
        clientVersion: Int,
        payload: JSONObject = alarmSyncPayload(alarm)
    ) {
        runCatching {
            SyncQueueStore(context).enqueue(
                operationType = operationType,
                entityType = "ALARM",
                entityId = alarm.id,
                payload = payload,
                clientVersion = clientVersion
            )
            syncScope.launch { OfflineSyncCoordinator(context.applicationContext).runOnce() }
        }
    }

    private fun alarmSyncPayload(alarm: Alarm) = JSONObject()
        .put("label", alarm.label)
        .put("hour", alarm.hour)
        .put("minute", alarm.minute)
        .put("enabled", alarm.enabled)
        .put("weekdays", JSONArray(alarm.weekdays.sorted()))
        .put("timezoneMode", alarm.timezoneMode.name)
        .put("fixedTimezone", alarm.fixedTimezone ?: JSONObject.NULL)
        .put("snoozeEnabled", true)
        .put("maxSnoozes", alarm.maxSnoozes)
        .put("snoozeMinutes", alarm.snoozeMinutes)
        .put("missionType", alarm.missionType.name)
        .put("difficulty", alarm.difficulty.name)
        .put("qrExpectedCode", alarm.qrExpectedCode ?: JSONObject.NULL)

    suspend fun repair(forceReschedule: Boolean = false) {
        val alarms = try {
            store.all()
        } catch (error: Exception) {
            Log.e(TAG, "Could not load alarms for recovery", error)
            return
        }

        runIndependently(
            items = alarms,
            action = { alarm ->
                if (!alarm.enabled) {
                    scheduler.cancel(alarm)
                } else if (forceReschedule || !scheduler.isScheduled(alarm)) {
                    scheduler.schedule(alarm)
                }
            },
            onFailure = { alarm, error ->
                Log.e(TAG, "Could not repair alarm schedule: ${alarm.id}", error)
            }
        )
    }

    private companion object {
        const val TAG = "AWERO.AlarmCoordinator"
        val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
