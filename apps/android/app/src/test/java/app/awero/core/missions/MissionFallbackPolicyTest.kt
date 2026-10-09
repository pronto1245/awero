package app.awero.core.missions

import app.awero.core.alarm.MissionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MissionFallbackPolicyTest {
    @Test
    fun cameraAndMotionMissionsFallbackToMath() {
        listOf(MissionType.QR, MissionType.STEPS, MissionType.PHOTO, MissionType.MIXED).forEach {
            assertEquals(MissionType.MATH, MissionFallbackPolicy.nextAfter(it))
        }
    }

    @Test
    fun mathDoesNotFallBackToItselfOrRepeat() {
        assertNull(MissionFallbackPolicy.nextAfter(MissionType.MATH))
    }
}
