package app.awero.core.wake

import android.content.Context
import app.awero.core.alarm.Alarm
import app.awero.core.alarm.AlarmScheduler
import app.awero.core.alarm.MissionType

class WakeFlowController(private val sessions: WakeSessionStore, private val context: Context) {
    enum class State { IDLE, RINGING, MISSION, COMPLETED, EMERGENCY_STOPPED }
    var state: State = State.IDLE
        private set
    var mission: MissionType = MissionType.MATH
        private set
    var snoozeCount: Int = 0
        private set
    var currentAlarm: Alarm? = null
        private set

    fun start(alarm: Alarm) {
        currentAlarm = alarm
        mission = alarm.missionType
        snoozeCount = 0
        sessions.start(alarm)
        state = State.RINGING
    }

    fun beginMission() {
        if (state == State.RINGING) {
            sessions.startMission()
            state = State.MISSION
        }
    }

    fun fallbackToMath() {
        if (state == State.MISSION) {
            sessions.markFallback()
            mission = MissionType.MATH
        }
    }

    fun completeMission() {
        if (state == State.MISSION) {
            sessions.complete()
            state = State.COMPLETED
        }
    }

    fun snooze(): Boolean {
        val alarm = currentAlarm ?: return false
        if (state != State.RINGING || snoozeCount >= alarm.maxSnoozes) return false
        snoozeCount++
        sessions.setSnoozeCount(snoozeCount)
        AlarmScheduler(context).scheduleSnooze(alarm, alarm.snoozeMinutes)
        state = State.IDLE
        return true
    }

    fun emergencyStop() {
        sessions.emergencyStop()
        state = State.EMERGENCY_STOPPED
    }
}
