package app.awero.core.wake

import app.awero.core.alarm.Alarm
import app.awero.core.missions.MissionType

class WakeFlowController(
    private val sessions: WakeSessionStore
) {
    enum class State { IDLE, RINGING, MISSION, COMPLETED, EMERGENCY_STOPPED }

    var state: State = State.IDLE
        private set
    var mission: MissionType = MissionType.MATH
        private set
    var snoozeCount: Int = 0
        private set

    fun start(alarm: Alarm) {
        mission = alarm.missionType
        snoozeCount = 0
        sessions.start(alarm)
        state = State.RINGING
    }

    fun beginMission() {
        if (state == State.RINGING) state = State.MISSION
    }

    fun completeMission() {
        if (state == State.MISSION) {
            sessions.complete()
            state = State.COMPLETED
        }
    }

    fun snooze(maxSnoozes: Int): Boolean {
        if (state != State.RINGING || snoozeCount >= maxSnoozes) return false
        snoozeCount++
        sessions.setSnoozeCount(snoozeCount)
        state = State.IDLE
        return true
    }

    fun emergencyStop() {
        sessions.emergencyStop()
        state = State.EMERGENCY_STOPPED
    }
}
