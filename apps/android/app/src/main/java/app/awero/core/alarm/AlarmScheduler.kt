package app.awero.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build
import android.os.Build.VERSION_CODES
import android.content.Context
import android.content.Intent
import app.awero.MainActivity
import java.util.Calendar
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.TimeZone

enum class AlarmReadiness {
    DISABLED,
    SCHEDULED,
    PERMISSION_REQUIRED,
    NOT_SCHEDULED,
    INVALID
}

internal fun resolveAlarmReadiness(
    enabled: Boolean,
    scheduleValid: Boolean,
    permissionsGranted: Boolean,
    scheduleRegistered: Boolean
): AlarmReadiness = when {
    !enabled -> AlarmReadiness.DISABLED
    !scheduleValid -> AlarmReadiness.INVALID
    !permissionsGranted -> AlarmReadiness.PERMISSION_REQUIRED
    scheduleRegistered -> AlarmReadiness.SCHEDULED
    else -> AlarmReadiness.NOT_SCHEDULED
}

internal fun validateAlarmSchedule(alarm: Alarm) {
    if (!alarm.enabled) return
    require(alarm.hour in 0..23 && alarm.minute in 0..59) {
        "Enter a valid alarm time."
    }
    require(alarm.weekdays.isNotEmpty()) {
        "Select at least one day for this repeating alarm."
    }
    require(alarm.weekdays.all { it in Calendar.SUNDAY..Calendar.SATURDAY }) {
        "Select valid days for this repeating alarm."
    }
    resolveAlarmTimeZone(alarm)
}

internal fun resolveAlarmTimeZone(alarm: Alarm): TimeZone {
    if (alarm.timezoneMode == TimezoneMode.DEVICE_LOCAL) return TimeZone.getDefault()
    val identifier = alarm.fixedTimezone
        ?: throw IllegalArgumentException("A valid fixed timezone is required for this alarm.")
    val zoneId = try {
        ZoneId.of(identifier)
    } catch (_: DateTimeException) {
        throw IllegalArgumentException("The selected alarm timezone is invalid.")
    }
    return TimeZone.getTimeZone(zoneId)
}

internal fun isAlarmScheduleRegistered(
    alarm: Alarm,
    exactAlarmAccessGranted: Boolean,
    pendingWeekdays: Set<Int>
): Boolean = alarm.enabled &&
    alarm.weekdays.isNotEmpty() &&
    exactAlarmAccessGranted &&
    alarm.weekdays.all(pendingWeekdays::contains)

internal fun nextAlarmOccurrence(alarm: Alarm, day: Int, now: Instant): ZonedDateTime {
    require(alarm.enabled) { "Disabled alarms cannot be scheduled." }
    validateAlarmSchedule(alarm)
    require(day in alarm.weekdays) { "The requested weekday is not enabled for this alarm." }

    val isoDay = when (day) {
        Calendar.SUNDAY -> 7
        else -> day - 1
    }
    val zone = resolveAlarmTimeZone(alarm).toZoneId()
    val nowInZone = now.atZone(zone)
    val daysAhead = (isoDay - nowInZone.dayOfWeek.value + 7) % 7
    var occurrenceDate = nowInZone.toLocalDate().plusDays(daysAhead.toLong())

    fun occurrence(date: java.time.LocalDate): ZonedDateTime =
        LocalDateTime.of(date, java.time.LocalTime.of(alarm.hour, alarm.minute)).atZone(zone)

    var candidate = occurrence(occurrenceDate)
    if (!candidate.toInstant().isAfter(now)) {
        occurrenceDate = occurrenceDate.plusWeeks(1)
        candidate = occurrence(occurrenceDate)
    }
    return candidate
}

class AlarmScheduler(private val context: Context) {
    private val manager = context.getSystemService(AlarmManager::class.java)

    fun schedule(a: Alarm) {
        if (!a.enabled) {
            cancel(a)
            return
        }
        validateAlarmSchedule(a)
        requireExactAlarmAccess()
        cancel(a)
        a.weekdays.forEach { day ->
            val at = next(a, day)
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_ALARM
                putExtra(EXTRA_ID, a.id)
                putExtra(EXTRA_VERSION, a.version)
                putExtra(EXTRA_AT, at.timeInMillis)
            }
            val pending = PendingIntent.getBroadcast(
                context, code(a, day), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setAlarmClock(a, at.timeInMillis, pending)
        }
    }

    fun scheduleTest(a: Alarm, seconds: Long = 30) =
        scheduleOneShot(a, ACTION_TEST, testCode(a), seconds * 1000L)

    fun scheduleSnooze(a: Alarm, minutes: Int) =
        scheduleOneShot(a, ACTION_SNOOZE, snoozeCode(a), minutes.coerceAtLeast(1) * 60_000L)

    private fun scheduleOneShot(a: Alarm, action: String, requestCode: Int, delay: Long) {
        requireExactAlarmAccess()
        val at = System.currentTimeMillis() + delay
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_ID, a.id)
            putExtra(EXTRA_VERSION, a.version)
            putExtra(EXTRA_AT, at)
        }
        val pending = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactAndAllowWhileIdle(at, pending)
    }

    fun cancel(a: Alarm) {
        (1..7).forEach { day ->
            cancelPending(code(a, day), ACTION_ALARM)
        }
        cancelPending(testCode(a), ACTION_TEST)
        cancelSnooze(a)
        cancelShowIntent(a)
    }

    fun cancelSnooze(a: Alarm) {
        cancelPending(snoozeCode(a), ACTION_SNOOZE)
    }

    private fun cancelPending(requestCode: Int, action: String) {
        val intent = Intent(context, AlarmReceiver::class.java).apply { this.action = action }
        val pending = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) {
            manager.cancel(pending)
            pending.cancel()
        }
    }

    fun isScheduled(a: Alarm): Boolean {
        if (!a.enabled || a.weekdays.isEmpty()) return false
        val exactAlarmAccessGranted =
            Build.VERSION.SDK_INT < VERSION_CODES.S || manager.canScheduleExactAlarms()
        val pendingWeekdays = a.weekdays.filter { day ->
            val intent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_ALARM }
            PendingIntent.getBroadcast(
                context, code(a, day), intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) != null
        }.toSet()
        return isAlarmScheduleRegistered(a, exactAlarmAccessGranted, pendingWeekdays)
    }

    fun readiness(a: Alarm): AlarmReadiness {
        if (!a.enabled) return AlarmReadiness.DISABLED
        val valid = runCatching { validateAlarmSchedule(a) }.isSuccess
        val permissionsGranted = AlarmNotificationManager.hasAlarmAccess(context) &&
            (Build.VERSION.SDK_INT < VERSION_CODES.S || manager.canScheduleExactAlarms())
        return resolveAlarmReadiness(a.enabled, valid, permissionsGranted, isScheduled(a))
    }

    private fun setAlarmClock(alarm: Alarm, at: Long, operation: PendingIntent) {
        val showIntent = PendingIntent.getActivity(
            context,
            showCode(alarm),
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_ID, alarm.id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        manager.setAlarmClock(AlarmManager.AlarmClockInfo(at, showIntent), operation)
    }

    private fun cancelShowIntent(alarm: Alarm) {
        val pending = PendingIntent.getActivity(
            context,
            showCode(alarm),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pending?.cancel()
    }

    private fun setExactAndAllowWhileIdle(at: Long, pending: PendingIntent) {
        manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
    }

    private fun requireExactAlarmAccess() {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            throw IllegalStateException(context.getString(app.awero.R.string.alarm_error_permission_exact))
        }
    }

    private fun next(a: Alarm, day: Int): Calendar {
        val instant = nextAlarmOccurrence(a, day, Instant.now()).toInstant()
        return Calendar.getInstance(resolveAlarmTimeZone(a)).apply {
            timeInMillis = instant.toEpochMilli()
        }
    }

    private fun code(a: Alarm, day: Int) = a.id.hashCode() * 31 + day
    private fun showCode(a: Alarm) = a.id.hashCode() xor 0x5A5A
    private fun testCode(a: Alarm) = a.id.hashCode() xor 0x55AA
    private fun snoozeCode(a: Alarm) = a.id.hashCode() xor 0xAA55

    companion object {
        const val ACTION_ALARM = "app.awero.ALARM"
        const val ACTION_TEST = "app.awero.TEST_ALARM"
        const val ACTION_SNOOZE = "app.awero.SNOOZE_ALARM"
        const val EXTRA_ID = "alarm_id"
        const val EXTRA_VERSION = "alarm_version"
        const val EXTRA_AT = "scheduled_at"
        const val EXTRA_TEST = "test_alarm"
    }
}
