package app.awero

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import app.awero.core.alarm.AlarmCoordinator
import app.awero.ui.AweroApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var exactAlarmSettingsOpened = false
    private var fullScreenSettingsOpened = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AweroApp() }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        } else {
            requestExactAlarmAccess()
        }

        lifecycleScope.launch { runCatching { AlarmCoordinator(this@MainActivity).repair() } }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATIONS) requestExactAlarmAccess()
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        ) {
            requestFullScreenAlarmAccess()
        }
    }

    private fun requestExactAlarmAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            requestFullScreenAlarmAccess()
            return
        }
        val manager = getSystemService(AlarmManager::class.java)
        if (manager.canScheduleExactAlarms() || exactAlarmSettingsOpened) {
            if (manager.canScheduleExactAlarms()) requestFullScreenAlarmAccess()
            return
        }

        exactAlarmSettingsOpened = true
        runCatching {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        }
    }

    private fun requestFullScreenAlarmAccess() {
        if (Build.VERSION.SDK_INT < 34 || fullScreenSettingsOpened) return
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.canUseFullScreenIntent()) return

        fullScreenSettingsOpened = true
        runCatching {
            startActivity(
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        }
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 4100
    }
}
