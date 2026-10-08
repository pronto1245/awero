package app.awero.core.alarm

import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.awero.core.wake.WakeSessionStore
import app.awero.ui.MissionRuntimeScreen
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class MissionTimeoutEndToEndTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val alarms = AlarmStore(context)
    private val sessions = WakeSessionStore(context)
    private var activity: WakeAlarmActivity? = null
    private var alarm: Alarm? = null

    @After
    fun cleanup() = runBlocking {
        activity?.let { current -> instrumentation.runOnMainSync { current.finish() } }
        alarm?.let { current ->
            if (sessions.loadActive()?.alarmId == current.id) sessions.complete()
            alarms.delete(current.id)
        }
    }

    @Test
    fun stepsTimeoutRequestsOneFallbackAndCannotReportSuccess() {
        val current = launchHost()
        val fallback = CountDownLatch(1)
        val calls = AtomicInteger()
        var root: View? = null
        instrumentation.runOnMainSync {
            root = MissionRuntimeScreen.create(
                current, alarm!!.copy(missionType = MissionType.STEPS),
                onSuccess = { fail("Timeout must not count as mission success") },
                onFailure = { calls.incrementAndGet(); fallback.countDown() },
                timeoutMillis = 0
            )
            current.setContentView(root)
        }
        assertTrue("Steps did not request fallback", fallback.await(5, TimeUnit.SECONDS))
        instrumentation.runOnMainSync {
            views(root!!).filterIsInstance<TextView>().single { it.text.toString() == "I CAN'T WALK" }.performClick()
        }
        assertEquals(1, calls.get())
    }

    @Test
    fun leavingMissionCancelsItsPendingTimeout() {
        val current = launchHost()
        val fallback = CountDownLatch(1)
        instrumentation.runOnMainSync {
            val mission = MissionRuntimeScreen.create(
                current, alarm!!.copy(missionType = MissionType.STEPS),
                onSuccess = { fail("Detached mission must not report success") },
                onFailure = { fallback.countDown() }, timeoutMillis = 100
            )
            current.setContentView(mission)
            current.setContentView(TextView(current).apply { text = "NEXT MISSION" })
        }
        assertFalse("Detached mission requested a late fallback", fallback.await(300, TimeUnit.MILLISECONDS))
    }

    private fun launchHost(): WakeAlarmActivity {
        val saved = Alarm(
            id = UUID.randomUUID().toString(), version = 1, hour = 7, minute = 30,
            enabled = true, missionType = MissionType.MATH, difficulty = Difficulty.EASY
        )
        alarm = saved
        runBlocking { alarms.save(saved) }
        val intent = Intent(context, WakeAlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(AlarmScheduler.EXTRA_ID, saved.id)
            putExtra(AlarmScheduler.EXTRA_VERSION, saved.version)
            putExtra(AlarmScheduler.EXTRA_TEST, true)
        }
        val current = instrumentation.startActivitySync(intent) as WakeAlarmActivity
        activity = current
        val deadline = System.currentTimeMillis() + 5_000L
        while (System.currentTimeMillis() < deadline) {
            var ready = false
            instrumentation.runOnMainSync {
                ready = views(current.window.decorView).any { it is TextView && it.text.toString() == "Start mission" }
            }
            if (ready) return current
            Thread.sleep(50)
        }
        fail("Wake host did not finish initialization")
        return current
    }

    private fun views(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { views(view.getChildAt(it)) } else emptyList()
}
