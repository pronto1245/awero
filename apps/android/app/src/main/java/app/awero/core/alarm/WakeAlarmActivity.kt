package app.awero.core.alarm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import app.awero.core.wake.WakeFlowController
import app.awero.core.wake.WakeSessionStore
import app.awero.ui.WakeAlarmScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WakeAlarmActivity : ComponentActivity() {
    private lateinit var flow: WakeFlowController
    private var alarm: Alarm? = null
    private var alarmVersion: Int = -1
    private var scheduledAt: Long = System.currentTimeMillis()
    private var permissionPending = false
    private var testAlarm = false

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val id = intent.getStringExtra(AlarmScheduler.EXTRA_ID) ?: return finish()
        alarmVersion = intent.getIntExtra(AlarmScheduler.EXTRA_VERSION, -1)
        scheduledAt = intent.getLongExtra(AlarmScheduler.EXTRA_AT, System.currentTimeMillis())
        testAlarm = intent.getBooleanExtra(AlarmScheduler.EXTRA_TEST, false)

        lifecycleScope.launch {
            val current = withContext(Dispatchers.IO) { AlarmStore(this@WakeAlarmActivity).get(id) }
                ?: return@launch finish()
            alarm = current
            if (current.version != alarmVersion || !current.enabled) {
                AlarmRingingService.stop(this@WakeAlarmActivity, id, testAlarm)
                return@launch finish()
            }

            when {
                current.missionType == MissionType.QR &&
                    checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED -> {
                    permissionPending = true
                    requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
                }
                current.missionType == MissionType.STEPS &&
                    checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED -> {
                    permissionPending = true
                    requestPermissions(arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), REQUEST_ACTIVITY)
                }
                else -> startFlow(current)
            }
        }
    }

    private suspend fun startFlow(current: Alarm) {
        permissionPending = false
        flow = WakeFlowController(WakeSessionStore(this), this, testAlarm = testAlarm)
        flow.start(current, scheduledAt)
        setContentView(WakeAlarmScreen.create(this, flow))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val current = alarm ?: return finish()
        if (permissionPending) lifecycleScope.launch { startFlow(current) }
    }

    companion object {
        private const val REQUEST_CAMERA = 4101
        private const val REQUEST_ACTIVITY = 4102
    }
}
