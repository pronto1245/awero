package app.awero.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

                withContext(Dispatchers.Main) {
                    AlarmNotificationManager.show(context, id, version, at, intent.action == AlarmScheduler.ACTION_TEST)
                    context.startActivity(Intent(context, WakeAlarmActivity::class.java).apply {
                        putExtra(AlarmScheduler.EXTRA_ID, id)
                        putExtra(AlarmScheduler.EXTRA_VERSION, version)
                        putExtra(AlarmScheduler.EXTRA_AT, at)
                        putExtra(AlarmScheduler.EXTRA_TEST, intent.action == AlarmScheduler.ACTION_TEST)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    })
                }
            } finally {
                pending.finish()
            }
        }
    }
}
