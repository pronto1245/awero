package app.awero.core.alarm

import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AlarmScheduleValidationTest {
    @Test
    fun enabledAlarmRequiresAtLeastOneWeekday() {
        assertThrows(IllegalArgumentException::class.java) {
            validateAlarmSchedule(alarm(weekdays = emptySet()))
        }
    }

    @Test
    fun disabledAlarmMayHaveNoWeekdays() {
        validateAlarmSchedule(alarm(enabled = false, weekdays = emptySet()))
    }

    @Test
    fun rejectsInvalidTimeAndWeekday() {
        assertThrows(IllegalArgumentException::class.java) {
            validateAlarmSchedule(alarm(hour = 24))
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateAlarmSchedule(alarm(weekdays = setOf(0)))
        }
    }

    @Test
    fun scheduledStateRequiresExactAlarmPermissionEvenWhenPendingIntentsRemain() {
        val alarm = alarm(weekdays = setOf(1, 2, 3))
        assertEquals(
            false,
            isAlarmScheduleRegistered(alarm, exactAlarmAccessGranted = false, pendingWeekdays = setOf(1, 2, 3))
        )
    }

    @Test
    fun scheduledStateRequiresEveryWeekdayToBeRegistered() {
        val alarm = alarm(weekdays = setOf(1, 2, 3))
        assertEquals(
            false,
            isAlarmScheduleRegistered(alarm, exactAlarmAccessGranted = true, pendingWeekdays = setOf(1, 2))
        )
        assertEquals(
            true,
            isAlarmScheduleRegistered(alarm, exactAlarmAccessGranted = true, pendingWeekdays = setOf(1, 2, 3))
        )
    }

    @Test
    fun fixedTimezoneMustBeValidInsteadOfSilentlyFallingBackToGmt() {
        assertEquals("Europe/Berlin", resolveAlarmTimeZone(alarm(timezoneMode = TimezoneMode.FIXED, fixedTimezone = "Europe/Berlin")).id)
        assertThrows(IllegalArgumentException::class.java) {
            validateAlarmSchedule(alarm(timezoneMode = TimezoneMode.FIXED, fixedTimezone = "Not/A_Timezone"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateAlarmSchedule(alarm(timezoneMode = TimezoneMode.FIXED, fixedTimezone = null))
        }
    }

    private fun alarm(
        hour: Int = 7,
        minute: Int = 30,
        enabled: Boolean = true,
        weekdays: Set<Int> = setOf(1, 2, 3, 4, 5),
        timezoneMode: TimezoneMode = TimezoneMode.DEVICE_LOCAL,
        fixedTimezone: String? = null
    ) = Alarm(
        id = "validation-alarm",
        version = 1,
        hour = hour,
        minute = minute,
        enabled = enabled,
        weekdays = weekdays,
        timezoneMode = timezoneMode,
        fixedTimezone = fixedTimezone,
        missionType = MissionType.MATH,
        difficulty = Difficulty.MEDIUM
    )
}
