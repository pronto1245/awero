package app.awero.core.missions

enum class MissionState { READY, STARTED, IN_PROGRESS, SUCCESS, RETRY, TIMEOUT, FAILURE, FALLBACK }

interface Mission {
    fun start()
    fun validate(): Boolean
    fun retry()
}

class MissionEngine {
    var state: MissionState = MissionState.READY
        private set

    fun start(mission: Mission) {
        state = MissionState.STARTED
        mission.start()
        state = MissionState.IN_PROGRESS
    }

    fun validate(mission: Mission): Boolean {
        val success = mission.validate()
        state = if (success) MissionState.SUCCESS else MissionState.RETRY
        return success
    }

    fun fallback() {
        state = MissionState.FALLBACK
    }
}
