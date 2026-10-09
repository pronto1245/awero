package app.awero.core.storage

import app.awero.core.wake.WakeSession

object WakeSessionMapper {
    fun toEntity(s: WakeSession) = WakeSessionEntity(
        id = s.id,
        alarmId = s.alarmId,
        alarmVersion = s.alarmVersion,
        scheduledAt = s.scheduledAt,
        triggeredAt = s.triggeredAt,
        missionStartedAt = s.missionStartedAt,
        completedAt = s.completedAt,
        result = s.result,
        snoozeCount = s.snoozeCount,
        fallbackUsed = s.fallbackUsed,
        emergencyStop = s.emergencyStop,
        isTest = s.isTest
    )

    fun fromEntity(e: WakeSessionEntity) = WakeSession(
        id = e.id,
        alarmId = e.alarmId,
        alarmVersion = e.alarmVersion,
        scheduledAt = e.scheduledAt,
        triggeredAt = e.triggeredAt,
        missionStartedAt = e.missionStartedAt,
        completedAt = e.completedAt,
        result = e.result,
        snoozeCount = e.snoozeCount,
        fallbackUsed = e.fallbackUsed,
        emergencyStop = e.emergencyStop,
        isTest = e.isTest
    )
}
