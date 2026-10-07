package app.awero.core.alarm

enum class MissionType { MATH, QR, STEPS, PHOTO, MIXED }
enum class Difficulty { EASY, MEDIUM, HARD }
enum class TimezoneMode { DEVICE_LOCAL, FIXED }

data class Alarm(
    val id: String,
    val version: Int,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean,
    val timezoneMode: TimezoneMode,
    val fixedTimezone: String?,
    val missionType: MissionType,
    val difficulty: Difficulty,
    val maxSnoozes: Int,
    val snoozeMinutes: Int
)
