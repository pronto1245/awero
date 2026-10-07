package app.awero.core.wake

import android.content.Context
import app.awero.core.alarm.Alarm
import app.awero.core.alarm.AlarmScheduler
import app.awero.core.alarm.MissionType
import app.awero.core.statistics.StatisticsStore

class WakeFlowController(private val sessions: WakeSessionStore, private val context: Context, private val statistics: StatisticsStore = StatisticsStore(context), private val testAlarm: Boolean = false) {
    enum class State { IDLE, RINGING, MISSION, COMPLETED, EMERGENCY_STOPPED }
    var state: State = State.IDLE
        private set
    var mission: MissionType = MissionType.MATH
        private set
    var snoozeCount: Int = 0
        private set
    var currentAlarm: Alarm? = null
        private set

    suspend fun start(alarm: Alarm, scheduledAt: Long = System.currentTimeMillis()) {
        currentAlarm = alarm
        mission = alarm.missionType
        snoozeCount = 0
        val created = sessions.start(alarm, scheduledAt)
        if (created && !testAlarm) statistics.recordPlanned()
        state = State.RINGING
    }

    suspend fun beginMission() {
        if (state == State.RINGING) {
            sessions.startMission()
            state = State.MISSION
        }
    }

    suspend fun fallbackToMath() {
        if (state == State.MISSION) {
            sessions.markFallback()
            mission = MissionType.MATH
        }
    }

    suspend fun completeMission() {
        if (state == State.MISSION) {
            sessions.complete()?.let { if (!testAlarm) statistics.record(it) }
            state = State.COMPLETED
        }
    }

    suspend fun snooze(): Boolean {
        val alarm = currentAlarm ?: return false
        if (state != State.RINGING || snoozeCount >= alarm.maxSnoozes) return false
        snoozeCount++
        sessions.setSnoozeCount(snoozeCount)
        AlarmScheduler(context).scheduleSnooze(alarm, alarm.snoozeMinutes)
        state = State.IDLE
        return true
    }

    suspend fun emergencyStop() {
        sessions.emergencyStop()?.let { if (!testAlarm) statistics.record(it) }
        state = State.EMERGENCY_STOPPED
    }
}
