package app.awero.core.alarm

import android.app.Activity
import android.os.Bundle
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

        flow = WakeFlowController(WakeSessionStore(this), this)
        flow.start(alarm)
        setContentView(WakeAlarmScreen.create(this, flow))
    }
}
