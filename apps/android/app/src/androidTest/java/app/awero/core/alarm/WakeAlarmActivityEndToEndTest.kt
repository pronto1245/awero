package app.awero.core.alarm

import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.awero.core.wake.WakeSessionStore
import kotlinx.coroutines.runBlocking
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WakeAlarmActivityEndToEndTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val alarmStore = AlarmStore(context)
    private val sessions = WakeSessionStore(context)
    private var activity: WakeAlarmActivity? = null
    private var alarm: Alarm? = null

    @After
    fun cleanup() {
        activity?.let { current ->
            instrumentation.runOnMainSync { current.finish() }
        }
        alarm?.let { saved ->
            runBlocking {
                if (sessions.loadActive()?.alarmId == saved.id) {
                    sessions.emergencyStop()
                }
                alarmStore.delete(saved.id)
            }
        }
    }

    @Test
    fun alarmIntentOpensWakeScreenAndEmergencyStopPersists() = runBlocking {
        val currentAlarm = Alarm(
            id = UUID.randomUUID().toString(),
            version = 1,
            hour = 7,
            minute = 30,
            enabled = true,
            missionType = MissionType.MATH,
            difficulty = Difficulty.EASY
        )
        alarm = currentAlarm
        alarmStore.save(currentAlarm)

        val intent = Intent(context, WakeAlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(AlarmScheduler.EXTRA_ID, currentAlarm.id)
            putExtra(AlarmScheduler.EXTRA_VERSION, currentAlarm.version)
            putExtra(AlarmScheduler.EXTRA_AT, System.currentTimeMillis())
            putExtra(AlarmScheduler.EXTRA_TEST, true)
        }
        activity = instrumentation.startActivitySync(intent) as WakeAlarmActivity

        awaitCondition("wake session was not persisted after the alarm intent") {
            runBlocking { sessions.loadActive()?.alarmId == currentAlarm.id }
        }

        instrumentation.runOnMainSync {
            val root = activity!!.window.decorView
            assertNotNull(findText(root, "Start mission"))
            val emergencyStop = findText(root, "Emergency stop")
            assertNotNull(emergencyStop)
            emergencyStop!!.performClick()
        }

        awaitCondition("emergency stop did not close the active wake session") {
            runBlocking { sessions.loadActive() == null }
        }

        instrumentation.runOnMainSync {
            assertNotNull(findText(activity!!.window.decorView, "STOPPED"))
        }
    }

    private fun awaitCondition(message: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000L
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(50)
        }
        assertTrue(message, condition())
    }

    private fun findText(view: View, expected: String): TextView? {
        if (view is TextView && view.text.toString() == expected) return view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                findText(view.getChildAt(index), expected)?.let { return it }
            }
        }
        return null
    }
}
