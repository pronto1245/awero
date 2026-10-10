package app.awero.core.wake

enum class WakeSessionState {
    SCHEDULED, TRIGGERED, AWAKE, MISSION, VALIDATED, COMPLETED, MISSION_FAILED, FALLBACK
}

data class WakeSession(
    val id: String,
    val alarmId: String,
    val alarmVersion: Int,
    val scheduledAt: Long,
    var triggeredAt: Long? = null,
    var missionStartedAt: Long? = null,
    var completedAt: Long? = null,
    var result: String? = null,
    var snoozeCount: Int = 0,
    var fallbackUsed: Boolean = false,
    var emergencyStop: Boolean = false,
    var isTest: Boolean = false,
    val missionType: String? = null
)
