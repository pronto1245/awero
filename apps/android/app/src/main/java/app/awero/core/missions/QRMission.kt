package app.awero.core.missions

class QRMission(
    private val difficulty: Difficulty,
    private val expectedPayload: String
) : Mission {
    override fun start() {}
    override fun retry() {}
    override fun validate(): Boolean = false

    fun validatePayload(payload: String): Boolean = payload == expectedPayload
}
