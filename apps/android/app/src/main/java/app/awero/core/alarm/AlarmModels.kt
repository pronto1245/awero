package app.awero.core.alarm

enum class MissionType { MATH, QR, STEPS, PHOTO, MIXED }
enum class Difficulty { EASY, MEDIUM, HARD }
enum class TimezoneMode { DEVICE_LOCAL, FIXED }

data class Alarm(
    val id: String,
    var version: Int,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean,
    val weekdays: Set<Int> = (1..7).toSet(),
    val timezoneMode: TimezoneMode = TimezoneMode.DEVICE_LOCAL,
    val fixedTimezone: String? = null,
    val missionType: MissionType,
    val difficulty: Difficulty,
    val maxSnoozes: Int = 3,
    val snoozeMinutes: Int = 10,
    val qrExpectedCode: String? = null
)
