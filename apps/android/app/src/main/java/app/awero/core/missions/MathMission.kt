package app.awero.core.missions

import app.awero.core.alarm.Difficulty

data class MathProblem(val left:Int,val right:Int,val operation:Char) {
    val answer:Int get() = if (operation == '+') left + right else left - right
}

class MathMission(private val difficulty: Difficulty) {
    var problem: MathProblem? = null
        private set

    fun start() {
        val max = when(difficulty) { Difficulty.EASY -> 10; Difficulty.MEDIUM -> 25; Difficulty.HARD -> 50 }
        val a = (1..max).random()
        val b = (1..max).random()
        problem = MathProblem(a,b,if ((0..1).random() == 0) '+' else '-')
    }

    fun validate(answer:Int): Boolean = problem?.answer == answer
}
