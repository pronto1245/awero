package app.awero.core.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import app.awero.R
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object AlarmNotificationManager {
    internal const val CHANNEL_ID = "awero_alarm_v2"
    internal const val NOTIFICATION_ID = 7001

    fun build(context: Context, alarmId: String, version: Int, scheduledAt: Long, test: Boolean): Notification {
        ensureChannel(context)
        val intent = Intent(context, WakeAlarmActivity::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_VERSION, version)
            putExtra(AlarmScheduler.EXTRA_AT, scheduledAt)
            putExtra(AlarmScheduler.EXTRA_TEST, test)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pending = PendingIntent.getActivity(
            context,
            alarmId.hashCode() xor version,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(if (test) R.string.notification_test_title else R.string.notification_alarm_title))
            .setContentText(context.getString(if (test) R.string.notification_test_body else R.string.notification_alarm_body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(!test)
            .setAutoCancel(false)
            .setFullScreenIntent(pending, true)
            .setContentIntent(pending)
            .build()
    }

    fun hasAlarmAccess(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 34 ||
                context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent())

    fun requireAlarmAccess(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationManagerCompat.from(context).areNotificationsEnabled()
        ) {
            throw IllegalStateException(context.getString(R.string.alarm_error_permission_notifications))
        }
        if (Build.VERSION.SDK_INT >= 34 &&
            !context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        ) {
            throw IllegalStateException(context.getString(R.string.alarm_error_permission_fullscreen))
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
