package app.awero.core.wake

data class SnoozePolicy(val maxSnoozes: Int, val durationMinutes: Int)

class SnoozeEngine(private val policy: SnoozePolicy) {
    var count: Int = 0
        private set

    val canSnooze: Boolean
        get() = count < policy.maxSnoozes

    fun snooze(): Boolean {
        if (!canSnooze) return false
        count += 1
        return true
    }

    fun reset() {
        count = 0
    }
}
