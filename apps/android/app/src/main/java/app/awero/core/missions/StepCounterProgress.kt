package app.awero.core.missions

/** Converts the reboot-relative sensor total into steps taken during this mission. */
class StepCounterProgress(target: Int) {
    private val target = target.coerceAtLeast(1)
    private var baseline: Int? = null
    var steps: Int = 0
        private set

    fun update(totalSinceBoot: Int) {
        val initialTotal = baseline ?: totalSinceBoot.also { baseline = it }
        steps = (totalSinceBoot - initialTotal).coerceAtLeast(0)
    }

    val completed: Boolean get() = steps >= target
}
