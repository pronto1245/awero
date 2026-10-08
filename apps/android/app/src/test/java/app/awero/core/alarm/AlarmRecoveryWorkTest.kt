package app.awero.core.alarm

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmRecoveryWorkTest {
    @Test fun failedAlarmDoesNotPreventLaterRepairs() {
        val attempted = mutableListOf<String>()
        val failures = mutableListOf<String>()
        runIndependently(listOf("broken", "healthy"), { alarm ->
            attempted += alarm
            if (alarm == "broken") error("invalid schedule")
        }, { alarm, _ -> failures += alarm })
        assertEquals(listOf("broken", "healthy"), attempted)
        assertEquals(listOf("broken"), failures)
    }

    @Test fun reportingFailureDoesNotPreventLaterRepairs() {
        val attempted = mutableListOf<String>()
        runIndependently(listOf("broken", "healthy"), { alarm ->
            attempted += alarm
            if (alarm == "broken") error("invalid schedule")
        }, { _, _ -> error("logger failed") })
        assertEquals(listOf("broken", "healthy"), attempted)
    }

    @Test fun ringingStartsBeforeRescheduleAndSurvivesRescheduleFailure() {
        val events = mutableListOf<String>()
        val failures = mutableListOf<String>()
        deliverAlarm({ events += "ring" }, {
            events += "reschedule"
            error("exact alarm permission revoked")
        }, { failures += it.message.orEmpty() })
        assertEquals(listOf("ring", "reschedule"), events)
        assertEquals(listOf("exact alarm permission revoked"), failures)
    }

    @Test fun reschedulingStillRunsIfRingingStartFails() {
        val events = mutableListOf<String>()
        val failures = mutableListOf<String>()
        deliverAlarm({
            events += "ring"
            error("foreground service unavailable")
        }, { events += "reschedule" }, { failures += it.message.orEmpty() })
        assertEquals(listOf("ring", "reschedule"), events)
        assertEquals(listOf("foreground service unavailable"), failures)
    }
}
