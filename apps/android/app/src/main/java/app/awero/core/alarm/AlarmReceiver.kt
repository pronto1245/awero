package app.awero.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.awero.core.wake.WakeFlowController

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
        val version = intent.getIntExtra(AlarmScheduler.EXTRA_VERSION, -1)
        val scheduledAt = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, System.currentTimeMillis())

        val pendingResult = goAsync()
        try {
            val controller = WakeFlowController()
            val prefs = context.getSharedPreferences("awero_alarms", Context.MODE_PRIVATE)
            if (prefs.getInt("version:$alarmId", version) != version) return
            pendingResult.setResultCode(if (intent.action == AlarmScheduler.ACTION_TEST) 2 else 1)
        } finally {
            pendingResult.finish()
        }
    }
}
