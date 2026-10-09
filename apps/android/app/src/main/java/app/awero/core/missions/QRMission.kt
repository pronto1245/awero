package app.awero.core.missions

class QRMission(private val expectedPayload: String) {
    fun validatePayload(payload: String): Boolean = payload == expectedPayload
}
