package app.awero.core.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.UserManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmRecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val exactAccessChanged = intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        if (intent.action !in setOf(
                Intent.ACTION_LOCKED_BOOT_COMPLETED,
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_USER_UNLOCKED,
                AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
            )
        ) return
        if (exactAccessChanged && Build.VERSION.SDK_INT >= VERSION_CODES.S &&
            !context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        ) return

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
                    !context.getSystemService(UserManager::class.java).isUserUnlocked
                ) {
                    restoreBeforeUnlock(context)
                } else {
                    AlarmCoordinator(context).repair(forceReschedule = true)
                }
            } catch (error: Exception) {
                Log.e(TAG, "Could not repair alarm schedules after a system change", error)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun restoreBeforeUnlock(context: Context) {
        val schedules = DeviceProtectedAlarmScheduleStore(context).all()
        runIndependently(
            items = schedules,
            action = { AlarmScheduler(context).schedule(it) },
            onFailure = { schedule, error ->
                Log.e(TAG, "Could not restore direct-boot alarm schedule: ${schedule.id}", error)
            }
        )
    }

    private companion object {
        const val TAG = "AWERO.AlarmRecoveryReceiver"
    }
}
