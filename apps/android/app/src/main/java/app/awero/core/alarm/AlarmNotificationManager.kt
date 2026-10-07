package app.awero.core.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object AlarmNotificationManager {
    private const val CHANNEL_ID = "awero_alarm"
    private const val NOTIFICATION_ID = 7001

    fun show(context: Context, alarmId: String, version: Int, scheduledAt: Long, test: Boolean) {
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
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(if (test) "AWERO — Test Alarm" else "AWERO")
            .setContentText("Wake up. Stay up.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(!test)
            .setAutoCancel(test)
            .setFullScreenIntent(pending, true)
            .setContentIntent(pending)
            .setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun clear(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AWERO alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Wake-up alarms"
                setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI, null)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
