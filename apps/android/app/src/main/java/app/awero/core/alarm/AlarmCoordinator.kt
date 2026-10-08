package app.awero.core.alarm

import android.content.Context
import android.util.Log
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
        difficulty: Difficulty = Difficulty.MEDIUM
    ): Alarm {
        val alarm = Alarm(
            id = UUID.randomUUID().toString(),
            version = 1,
            hour = hour,
            minute = minute,
            enabled = true,
            missionType = missionType,
            difficulty = difficulty
        )
        AlarmNotificationManager.requireAlarmAccess(context)
        try {
            store.save(alarm)
            scheduler.schedule(alarm)
            return alarm
        } catch (error: Exception) {
            scheduler.cancel(alarm)
            store.delete(alarm.id)
            throw error
        }
    }

    suspend fun update(alarm: Alarm): Alarm {
        val next = alarm.copy(version = alarm.version + 1)
        AlarmNotificationManager.requireAlarmAccess(context)
        store.save(next)
        try {
            scheduler.schedule(next)
            return next
        } catch (error: Exception) {
            store.save(alarm)
            runCatching { scheduler.schedule(alarm) }
            throw error
        }
    }

    suspend fun delete(alarm: Alarm) {
        scheduler.cancel(alarm)
        store.delete(alarm.id)
    }

    fun test(alarm: Alarm) {
        AlarmNotificationManager.requireAlarmAccess(context)
        scheduler.scheduleTest(alarm)
    }

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
    }
}
