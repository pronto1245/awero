package app.awero.core.alarm

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Calendar
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
    fun readinessRequiresEnabledAlarmValidSchedulePermissionsAndRegistration() {
        assertEquals(AlarmReadiness.DISABLED, resolveAlarmReadiness(false, true, true, true))
        assertEquals(AlarmReadiness.INVALID, resolveAlarmReadiness(true, false, true, true))
        assertEquals(AlarmReadiness.PERMISSION_REQUIRED, resolveAlarmReadiness(true, true, false, true))
        assertEquals(AlarmReadiness.NOT_SCHEDULED, resolveAlarmReadiness(true, true, true, false))
        assertEquals(AlarmReadiness.SCHEDULED, resolveAlarmReadiness(true, true, true, true))
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
    fun selectsTodayWhenAlarmTimeHasNotPassed() {
        val alarm = alarm(
            weekdays = setOf(Calendar.SUNDAY),
            timezoneMode = TimezoneMode.FIXED,
            fixedTimezone = "Europe/Berlin"
        )
        val now = ZonedDateTimeForTest.instant(2026, 1, 4, 7, 0, 0, "Europe/Berlin")
        val next = nextAlarmOccurrence(alarm, Calendar.SUNDAY, now)
        assertEquals(LocalDateTime.of(2026, 1, 4, 7, 30), next.toLocalDateTime())
    }

    @Test
    fun exactAlarmTimeAdvancesToNextWeek() {
        val alarm = alarm(
            weekdays = setOf(Calendar.SUNDAY),
            timezoneMode = TimezoneMode.FIXED,
            fixedTimezone = "Europe/Berlin"
        )
        val now = ZonedDateTimeForTest.instant(2026, 1, 4, 7, 30, 0, "Europe/Berlin")
        val next = nextAlarmOccurrence(alarm, Calendar.SUNDAY, now)
        assertEquals(LocalDateTime.of(2026, 1, 11, 7, 30), next.toLocalDateTime())
    }

    @Test
    fun nonexistentSpringForwardTimeMovesByTheDstGap() {
        val alarm = alarm(
            hour = 2,
            minute = 30,
            weekdays = setOf(Calendar.SUNDAY),
            timezoneMode = TimezoneMode.FIXED,
            fixedTimezone = "Europe/Berlin"
        )
        val now = ZonedDateTimeForTest.instant(2026, 3, 28, 12, 0, 0, "Europe/Berlin")
        val next = nextAlarmOccurrence(alarm, Calendar.SUNDAY, now)
        assertEquals(LocalDateTime.of(2026, 3, 29, 3, 30), next.toLocalDateTime())
    }

    @Test
    fun repeatedFallBackTimeUsesTheEarlierOccurrence() {
        val alarm = alarm(
            hour = 2,
            minute = 30,
            weekdays = setOf(Calendar.SUNDAY),
            timezoneMode = TimezoneMode.FIXED,
            fixedTimezone = "Europe/Berlin"
        )
        val now = ZonedDateTimeForTest.instant(2026, 10, 24, 12, 0, 0, "Europe/Berlin")
        val next = nextAlarmOccurrence(alarm, Calendar.SUNDAY, now)
        assertEquals(LocalDateTime.of(2026, 10, 25, 2, 30), next.toLocalDateTime())
        assertEquals(ZoneOffset.ofHours(2), next.offset)
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

    private object ZonedDateTimeForTest {
        fun instant(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int, zone: String) =
            java.time.ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZoneId.of(zone)).toInstant()
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
