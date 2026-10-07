package app.awero.core.wake

import app.awero.core.alarm.Alarm
import app.awero.core.alarm.MissionType

class WakeFlowController {
    var current: WakeSession? = null
        private set

    private var fallbackEngine: FallbackEngine? = null
    private var snoozeEngine: SnoozeEngine? = null

    fun begin(alarm: Alarm, scheduledAt: Long) {
        current = WakeSession(
            id = java.util.UUID.randomUUID().toString(),
            alarmId = alarm.id,
            alarmVersion = alarm.version,
            scheduledAt = scheduledAt
        )
        fallbackEngine = FallbackEngine(alarm.missionType)
        snoozeEngine = SnoozeEngine(SnoozePolicy(alarm.maxSnoozes, alarm.snoozeMinutes))
    }

    fun startMission() {
        current?.missionStartedAt = System.currentTimeMillis()
    }

    fun snooze(): Boolean {
        val engine = snoozeEngine ?: return false
        if (!engine.snooze()) return false
        current?.snoozeCount = engine.count
        return true
    }

    fun fallback(from: MissionType): MissionType? {
        val next = fallbackEngine?.next(from) ?: return null
        current?.fallbackUsed = true
        return next
    }

    fun complete() {
        current?.completedAt = System.currentTimeMillis()
        current?.result = "COMPLETED"
    }

    fun emergencyStop() {
        current?.emergencyStop = true
        current?.result = "EMERGENCY_STOP"
        current?.completedAt = System.currentTimeMillis()
    }
}
