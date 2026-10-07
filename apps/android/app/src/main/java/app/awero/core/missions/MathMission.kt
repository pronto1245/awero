package app.awero.core.missions

class MathMission(private val difficulty: Difficulty) : Mission {
    val left: Int
    val right: Int

    init {
        when (difficulty) {
            Difficulty.EASY -> { left = 7; right = 5 }
            Difficulty.MEDIUM -> { left = 17; right = 8 }
            Difficulty.HARD -> { left = 27; right = 16 }
        }
    }

    override fun start() {}
    override fun retry() {}
    override fun validate(): Boolean = false

    val answer: Int get() = left + right
}

enum class Difficulty { EASY, MEDIUM, HARD }
