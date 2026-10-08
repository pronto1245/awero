package app.awero.core.alarm

import android.content.Context
import java.util.UUID

class AlarmCoordinator(
    context: Context,
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
        store.save(alarm)
        scheduler.schedule(alarm)
        return alarm
    }

    suspend fun update(alarm: Alarm): Alarm {
        val next = alarm.copy(version = alarm.version + 1)
        store.save(next)
        scheduler.schedule(next)
        return next
    }

    suspend fun delete(alarm: Alarm) {
        scheduler.cancel(alarm)
        store.delete(alarm.id)
    }

    fun test(alarm: Alarm) {
        scheduler.scheduleTest(alarm)
    }

    suspend fun repair(forceReschedule: Boolean = false) {
        store.all().forEach { alarm ->
            if (!alarm.enabled) {
                scheduler.cancel(alarm)
            } else if (forceReschedule || !scheduler.isScheduled(alarm)) {
                scheduler.schedule(alarm)
            }
        }
    }
}
