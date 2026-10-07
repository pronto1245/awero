package app.awero.core.wake

class EmergencyStopController {
    var stopped: Boolean = false
        private set

    fun stop() { stopped = true }
    fun reset() { stopped = false }
}
