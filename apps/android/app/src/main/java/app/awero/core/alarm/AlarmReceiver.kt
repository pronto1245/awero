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

        AlarmScheduler(context).schedule(alarm)

        val wake = Intent(context, WakeAlarmActivity::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ID, id)
            putExtra(AlarmScheduler.EXTRA_VERSION, version)
            putExtra(AlarmScheduler.EXTRA_AT, at)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(wake)
    }
}
