package app.awero.core.missions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QRMissionTest {
    @Test
    fun onlyTheSavedPayloadIsAccepted() {
        val mission = QRMission("awero://wake/room?code=a+b")

        assertTrue(mission.validatePayload("awero://wake/room?code=a+b"))
        assertFalse(mission.validatePayload("awero://wake/room?code=a b"))
        assertFalse(mission.validatePayload("different"))
    }
}
