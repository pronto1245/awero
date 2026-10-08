package app.awero.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build
import android.os.Build.VERSION_CODES
import android.content.Context
import android.content.Intent
import java.util.Calendar
import java.time.DateTimeException
import java.time.ZoneId
import java.util.TimeZone

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
            set(at.timeInMillis, pending)
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
        set(at, pending)
    }

    fun cancel(a: Alarm) {
        (1..7).forEach { day ->
            cancelPending(code(a, day), ACTION_ALARM)
        }
        cancelPending(testCode(a), ACTION_TEST)
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
        return a.weekdays.all { day ->
            val intent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_ALARM }
            PendingIntent.getBroadcast(
                context, code(a, day), intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) != null
        }
    }

    private fun set(at: Long, pending: PendingIntent) {
        manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
    }

    private fun requireExactAlarmAccess() {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            throw IllegalStateException(
                "Allow AWERO to set alarms and reminders in Android Settings to use reliable alarms."
            )
        }
    }

    private fun next(a: Alarm, day: Int): Calendar {
        val tz = resolveAlarmTimeZone(a)
        val now = Calendar.getInstance(tz)
        val target = Calendar.getInstance(tz).apply {
            set(Calendar.HOUR_OF_DAY, a.hour)
            set(Calendar.MINUTE, a.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, day)
        }
        if (target.timeInMillis <= now.timeInMillis) target.add(Calendar.WEEK_OF_YEAR, 1)
        return target
    }

    private fun code(a: Alarm, day: Int) = a.id.hashCode() * 31 + day
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
