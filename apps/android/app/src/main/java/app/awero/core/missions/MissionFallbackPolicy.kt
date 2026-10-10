package app.awero.core.missions

import app.awero.core.alarm.MissionType

/** The persisted wake session records whether this single safe fallback was used. */
object MissionFallbackPolicy {
    fun nextAfter(mission: MissionType): MissionType? = when (mission) {
        MissionType.MATH -> null
        MissionType.QR, MissionType.STEPS, MissionType.PHOTO, MissionType.MIXED -> MissionType.MATH
    }
}
