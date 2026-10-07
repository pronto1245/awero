package app.awero.core.storage

import app.awero.core.alarm.Alarm
import app.awero.core.alarm.Difficulty
import app.awero.core.alarm.MissionType
import app.awero.core.alarm.TimezoneMode

object AlarmMapper {
    fun toEntity(a: Alarm) = AlarmEntity(
        id = a.id,
        version = a.version,
        hour = a.hour,
        minute = a.minute,
        enabled = a.enabled,
        weekdays = a.weekdays.sorted().joinToString(","),
        timezoneMode = a.timezoneMode.name,
        fixedTimezone = a.fixedTimezone,
        missionType = a.missionType.name,
        difficulty = a.difficulty.name,
        maxSnoozes = a.maxSnoozes,
        snoozeMinutes = a.snoozeMinutes,
        qrExpectedCode = a.qrExpectedCode
    )

    fun fromEntity(e: Alarm) = e
}
