package app.awero.core.missions

class FallbackEngine {
    private val attempts = mutableListOf<MissionType>()

    fun reset() = attempts.clear()

    fun nextAfter(mission: MissionType): MissionType? {
        if (attempts.lastOrNull() != mission) attempts += mission
        return when (mission) {
            MissionType.PHOTO -> MissionType.QR
            MissionType.QR -> MissionType.MATH
            MissionType.STEPS -> MissionType.MATH
            MissionType.MIXED -> MissionType.MATH
            MissionType.MATH -> null
        }
    }
}
