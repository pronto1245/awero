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
        qrExpectedCode = a.qrExpectedCode,
        label = a.label,
        snoozeEnabled = a.snoozeEnabled
    )

    fun fromEntity(e: AlarmEntity) = Alarm(
        id = e.id,
        version = e.version,
        hour = e.hour,
        minute = e.minute,
        enabled = e.enabled,
        weekdays = e.weekdays.split(",").filter { it.isNotBlank() }.map { it.toInt() }.toSet(),
        timezoneMode = TimezoneMode.valueOf(e.timezoneMode),
        fixedTimezone = e.fixedTimezone,
        missionType = MissionType.valueOf(e.missionType),
        difficulty = Difficulty.valueOf(e.difficulty),
        maxSnoozes = e.maxSnoozes,
        snoozeMinutes = e.snoozeMinutes,
        qrExpectedCode = e.qrExpectedCode,
        label = e.label,
        snoozeEnabled = e.snoozeEnabled
    )
}
