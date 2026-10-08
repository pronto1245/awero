package app.awero.core.alarm

import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.awero.core.wake.WakeSessionStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class WakeMissionRestoreEndToEndTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val alarms = AlarmStore(context)
    private val sessions = WakeSessionStore(context)
    private val savedAlarms = mutableListOf<Alarm>()
    private var activity: WakeAlarmActivity? = null

    @After
    fun cleanup() = runBlocking {
        activity?.let { current -> instrumentation.runOnMainSync { current.finish() } }
        if (savedAlarms.any { it.id == sessions.loadActive()?.alarmId }) sessions.complete()
        savedAlarms.forEach { alarms.delete(it.id) }
    }

    @Test
    fun repeatedDeliveryShowsRestoredMathFallbackAndCanComplete() = runBlocking {
        val original = alarm(MissionType.QR)
        val incoming = alarm(MissionType.MATH)
        savedAlarms.addAll(listOf(original, incoming))
        savedAlarms.forEach { alarms.save(it) }
        assertTrue(sessions.start(original, 1_000L))
        assertTrue(sessions.startMission())
        assertTrue(sessions.markFallback())
        val sessionId = sessions.loadActive()!!.id

        // A duplicate delivery must resume the persisted mission, even when the
        // incoming alarm has a different type. No camera or network is needed.
        val intent = Intent(context, WakeAlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(AlarmScheduler.EXTRA_ID, incoming.id)
            putExtra(AlarmScheduler.EXTRA_VERSION, incoming.version)
            putExtra(AlarmScheduler.EXTRA_AT, 2_000L)
            putExtra(AlarmScheduler.EXTRA_TEST, true)
        }
        activity = instrumentation.startActivitySync(intent) as WakeAlarmActivity
        awaitCondition("Restored mission was not rendered") {
            var ready = false
            instrumentation.runOnMainSync {
                ready = views(activity!!.window.decorView).any { it is TextView && it.text.toString() == "CHECK" }
            }
            ready
        }
        assertEquals(sessionId, sessions.loadActive()?.id)
        assertEquals(original.id, sessions.loadActive()?.alarmId)

        instrumentation.runOnMainSync {
            val children = views(activity!!.window.decorView)
            assertFalse(children.any { it is TextView && it.text.toString() == "Start mission" })
            val pattern = Regex("(\\d+) ([+-]) (\\d+) = \\?")
            val problem = children.filterIsInstance<TextView>()
                .mapNotNull { pattern.matchEntire(it.text.toString()) }.single()
            val left = problem.groupValues[1].toInt()
            val right = problem.groupValues[3].toInt()
            val answer = if (problem.groupValues[2] == "+") left + right else left - right
            children.filterIsInstance<EditText>().single().setText(answer.toString())
            children.filterIsInstance<TextView>().single { it.text.toString() == "CHECK" }.performClick()
        }
        awaitCondition("Restored mission could not complete") { runBlocking { sessions.loadActive() == null } }
        awaitCondition("Completion result was not rendered") {
            var completed = false
            instrumentation.runOnMainSync {
                completed = views(activity!!.window.decorView).any { it is TextView && it.text.toString() == "YOU'RE UP" }
            }
            completed
        }
        val completed = sessions.load().single { it.id == sessionId }
        assertEquals("SUCCESS", completed.result)
        assertTrue(completed.fallbackUsed)
    }

    private fun alarm(type: MissionType) = Alarm(
        id = UUID.randomUUID().toString(), version = 1, hour = 7, minute = 30,
        enabled = true, missionType = type, difficulty = Difficulty.EASY
    )

    private fun views(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { views(view.getChildAt(it)) } else emptyList()

    private fun awaitCondition(message: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000L
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(50)
        }
        assertTrue(message, condition())
    }
}
