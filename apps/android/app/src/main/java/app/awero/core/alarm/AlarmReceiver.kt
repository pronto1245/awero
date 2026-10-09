package app.awero.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.util.Log
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
                val isTestAlarm = intent.action == AlarmScheduler.ACTION_TEST ||
                    intent.getBooleanExtra(AlarmScheduler.EXTRA_TEST, false)
                val schedule = if (context.getSystemService(UserManager::class.java).isUserUnlocked) {
                    AlarmStore(context).get(id)?.toSchedule()
                } else {
                    if (intent.action != AlarmScheduler.ACTION_ALARM) return@launch
                    DeviceProtectedAlarmScheduleStore(context).get(id)
                } ?: return@launch
                if (schedule.version != version || !schedule.enabled) return@launch

                val startRinging = {
                    AlarmRingingService.start(
                        context = context,
                        alarmId = id,
                        version = version,
                        scheduledAt = at,
                        test = isTestAlarm
                    )
                }
                if (intent.action == AlarmScheduler.ACTION_ALARM) {
                    deliverAlarm(
                        startRinging = startRinging,
                        reschedule = { AlarmScheduler(context).schedule(schedule) },
                        onFailure = { error ->
                            Log.e(TAG, "Alarm fired, but its next occurrence could not be scheduled: $id", error)
                        }
                    )
                } else {
                    try { startRinging() } catch (error: Exception) {
                        Log.e(TAG, "Could not start alarm ringing: $id", error)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "AWERO.AlarmReceiver"
    }
}
