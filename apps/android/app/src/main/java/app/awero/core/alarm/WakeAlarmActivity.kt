package app.awero.core.alarm

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.content.pm.PackageManager
import app.awero.core.wake.WakeFlowController
import app.awero.core.wake.WakeSessionStore
import app.awero.ui.WakeAlarmScreen

class WakeAlarmActivity : Activity() {
    private lateinit var flow: WakeFlowController

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val id = intent.getStringExtra(AlarmScheduler.EXTRA_ID) ?: return finish()
        val version = intent.getIntExtra(AlarmScheduler.EXTRA_VERSION, -1)
        val alarm = AlarmStore(this).get(id) ?: return finish()
        if (alarm.version != version || !alarm.enabled) return finish()

        if (alarm.missionType == MissionType.QR &&
            checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 4101)
        }
        if (alarm.missionType == MissionType.STEPS &&
            checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 4102)
        }

        flow = WakeFlowController(WakeSessionStore(this), this)
        flow.start(alarm)
        setContentView(WakeAlarmScreen.create(this, flow))
    }
}
