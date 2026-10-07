package app.awero.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(AlarmScheduler.EXTRA_ID) ?: return
        val version = intent.getIntExtra(AlarmScheduler.EXTRA_VERSION, -1)
        val at = intent.getLongExtra(AlarmScheduler.EXTRA_AT, System.currentTimeMillis())
        val alarm = AlarmStore(context).get(id) ?: return
        if (alarm.version != version || !alarm.enabled) return

        if (intent.action == AlarmScheduler.ACTION_ALARM) {
            AlarmScheduler(context).schedule(alarm)
        }

        AlarmNotificationManager.show(context, id, version, at, intent.action == AlarmScheduler.ACTION_TEST)

        val wake = Intent(context, WakeAlarmActivity::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ID, id)
            putExtra(AlarmScheduler.EXTRA_VERSION, version)
            putExtra(AlarmScheduler.EXTRA_AT, at)
            putExtra(AlarmScheduler.EXTRA_TEST, intent.action == AlarmScheduler.ACTION_TEST)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(wake)
    }
}
