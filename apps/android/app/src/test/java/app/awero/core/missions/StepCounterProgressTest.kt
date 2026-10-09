package app.awero.core.missions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StepCounterProgressTest {
    @Test
    fun countsOnlyStepsAfterMissionStartsIncludingZeroSensorBaseline() {
        val progress = StepCounterProgress(target = 3)
        progress.update(0)
        assertEquals(0, progress.steps)
        progress.update(2)
        assertEquals(2, progress.steps)
        assertFalse(progress.completed)
        progress.update(3)
        assertEquals(3, progress.steps)
        assertTrue(progress.completed)
    }

    @Test
    fun counterRegressionCannotCreateSteps() {
        val progress = StepCounterProgress(target = 2)
        progress.update(100)
        progress.update(102)
        progress.update(90)
        assertEquals(0, progress.steps)
        assertFalse(progress.completed)
    }
}
