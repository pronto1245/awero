package app.awero.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmRecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            // Production implementation loads alarms from local DB and reconciles all schedules.
            // This receiver intentionally does not depend on network.
        }
    }
}
