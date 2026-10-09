package app.awero.core.missions

import app.awero.core.alarm.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MathMissionTest {
    @Test
    fun onlyTheComputedAnswerIsAccepted() {
        val mission = MathMission(Difficulty.EASY)
        mission.start()
        val problem = requireNotNull(mission.problem)

        assertTrue(mission.validate(problem.answer))
        assertFalse(mission.validate(problem.answer + 1))
    }

    @Test
    fun difficultyBoundsOperands() {
        for ((difficulty, bound) in listOf(Difficulty.EASY to 10, Difficulty.MEDIUM to 25, Difficulty.HARD to 50)) {
            val mission = MathMission(difficulty)
            repeat(50) {
                mission.start()
                val problem = requireNotNull(mission.problem)
                assertTrue(problem.left in 1..bound)
                assertTrue(problem.right in 1..bound)
                assertEquals(problem.operation == '+', mission.validate(problem.left + problem.right))
            }
        }
    }
}
