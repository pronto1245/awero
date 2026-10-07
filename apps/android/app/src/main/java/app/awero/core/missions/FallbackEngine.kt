package app.awero.core.missions

class FallbackEngine(primary: MissionType) {
    private val chain = when (primary) {
        MissionType.PHOTO -> listOf(MissionType.PHOTO, MissionType.QR, MissionType.MATH)
        MissionType.STEPS -> listOf(MissionType.STEPS, MissionType.QR, MissionType.MATH)
        MissionType.QR -> listOf(MissionType.QR, MissionType.MATH)
        MissionType.MIXED -> listOf(MissionType.MIXED, MissionType.QR, MissionType.MATH)
        MissionType.MATH -> listOf(MissionType.MATH)
    }

    fun next(after: MissionType): MissionType? {
        val index = chain.indexOf(after)
        return if (index >= 0 && index + 1 < chain.size) chain[index + 1] else null
    }
}
