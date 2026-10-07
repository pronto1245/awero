package app.awero.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar
import java.util.TimeZone

class AlarmScheduler(private val context: Context) {
    private val manager = context.getSystemService(AlarmManager::class.java)

    fun schedule(a: Alarm) {
        cancel(a)
        if (!a.enabled) return
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
        scheduleOneShot(a, ACTION_TEST, a.id.hashCode() xor 0x55AA, seconds * 1000L)

    fun scheduleSnooze(a: Alarm, minutes: Int) =
        scheduleOneShot(a, ACTION_SNOOZE, a.id.hashCode() xor 0xAA55, minutes.coerceAtLeast(1) * 60_000L)

    private fun scheduleOneShot(a: Alarm, action: String, requestCode: Int, delay: Long) {
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
            val intent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_ALARM }
            val pending = PendingIntent.getBroadcast(
                context, code(a, day), intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pending != null) manager.cancel(pending)
        }
    }

    fun isScheduled(a: Alarm): Boolean = a.weekdays.all { day ->
        val intent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_ALARM }
        PendingIntent.getBroadcast(
            context, code(a, day), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) != null
    }

    private fun set(at: Long, pending: PendingIntent) {
        if (manager.canScheduleExactAlarms()) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }

    private fun next(a: Alarm, day: Int): Calendar {
        val tz = if (a.timezoneMode == TimezoneMode.FIXED && a.fixedTimezone != null)
            TimeZone.getTimeZone(a.fixedTimezone) else TimeZone.getDefault()
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

    companion object {
        const val ACTION_ALARM = "app.awero.ALARM"
        const val ACTION_TEST = "app.awero.TEST_ALARM"
        const val ACTION_SNOOZE = "app.awero.SNOOZE_ALARM"
        const val EXTRA_ID = "alarm_id"
        const val EXTRA_VERSION = "alarm_version"
        const val EXTRA_AT = "scheduled_at"
    }
}
