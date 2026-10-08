package app.awero.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val id = intent.getStringExtra(AlarmScheduler.EXTRA_ID) ?: return@launch
                val version = intent.getIntExtra(AlarmScheduler.EXTRA_VERSION, -1)
                val at = intent.getLongExtra(AlarmScheduler.EXTRA_AT, System.currentTimeMillis())
                val alarm = AlarmStore(context).get(id) ?: return@launch
                if (alarm.version != version || !alarm.enabled) return@launch

                if (intent.action == AlarmScheduler.ACTION_ALARM) {
                    AlarmScheduler(context).schedule(alarm)
                }

                AlarmRingingService.start(
                    context = context,
                    alarmId = id,
                    version = version,
                    scheduledAt = at,
                    test = intent.action == AlarmScheduler.ACTION_TEST
                )
            } finally {
                pending.finish()
            }
        }
    }
}
