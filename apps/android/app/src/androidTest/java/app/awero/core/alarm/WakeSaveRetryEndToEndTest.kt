package app.awero.core.alarm

import android.content.Context
import app.awero.R
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.awero.core.storage.AweroDatabase
import app.awero.core.statistics.StatisticsStore
import app.awero.core.wake.WakeFlowController
import app.awero.core.wake.WakeSessionStore
import app.awero.ui.WakeAlarmScreen
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class WakeSaveRetryEndToEndTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val databaseName = "awero-wake-save-retry.db"
    private val database = AweroDatabase.createForTesting(context, databaseName)
    private val testAlarms = AlarmStore(context, database)
    private val testSessions = WakeSessionStore(context, database)
    private val defaultAlarms = AlarmStore(context)
    private val defaultSessions = WakeSessionStore(context)
    private var activity: WakeAlarmActivity? = null
    private var alarm: Alarm? = null

    @After
    fun cleanup() = runBlocking {
        activity?.let { current -> instrumentation.runOnMainSync { current.finish() } }
        alarm?.let { current ->
            if (defaultSessions.loadActive()?.alarmId == current.id) defaultSessions.complete()
            defaultAlarms.delete(current.id)
            testAlarms.delete(current.id)
        }
        database.close()
        context.deleteDatabase(databaseName)
        Unit
    }

    @Test
    fun validatedMathMissionCanRetryAfterCompletionWriteFails() = runBlocking {
        val currentAlarm = Alarm(
            id = UUID.randomUUID().toString(), version = 1, hour = 7, minute = 30,
            enabled = true, missionType = MissionType.MATH, difficulty = Difficulty.EASY
        )
        alarm = currentAlarm
        testAlarms.save(currentAlarm)
        defaultAlarms.save(currentAlarm)

        val intent = Intent(context, WakeAlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(AlarmScheduler.EXTRA_ID, currentAlarm.id)
            putExtra(AlarmScheduler.EXTRA_VERSION, currentAlarm.version)
            putExtra(AlarmScheduler.EXTRA_TEST, true)
        }
        activity = instrumentation.startActivitySync(intent) as WakeAlarmActivity
        awaitCondition("Wake host did not finish initialization") {
            hasText(context.getString(R.string.wake_start))
        }

        var completionAttempts = 0
        val flow = WakeFlowController(
            sessions = testSessions,
            context = context,
            statistics = StatisticsStore(context, database),
            testAlarm = true,
            alarmStore = testAlarms,
            completeSession = {
                completionAttempts += 1
                if (completionAttempts == 1) error("Simulated temporary save failure")
                testSessions.complete()
            }
        )
        flow.start(currentAlarm, 1_000L)
        assertTrue(flow.beginMission())
        instrumentation.runOnMainSync {
            activity!!.setContentView(WakeAlarmScreen.create(activity!!, flow))
        }

        instrumentation.waitForIdleSync()
        instrumentation.uiAutomation.waitForIdle(500, 5_000)
        instrumentation.runOnMainSync {
            val buttons = views(activity!!.window.decorView).filterIsInstance<Button>()
            val visibleBounds = android.graphics.Rect()
            val critical = buttons.filter { it.text.toString() in listOf("0", "✓") }
            assertEquals("Math control set was not rendered", 2, critical.size)
            for (button in critical) {
                assertTrue("Critical control was clipped: ${button.text}", button.getGlobalVisibleRect(visibleBounds))
                assertEquals("Critical control was partly hidden: ${button.text}", button.height, visibleBounds.height())
            }
            val emergency = views(activity!!.window.decorView).single { it.contentDescription == context.getString(R.string.wake_emergency_stop) + ". " + context.getString(R.string.wake_emergency_stop_hint) }
            assertTrue("Emergency stop card was not visible", emergency.getGlobalVisibleRect(visibleBounds))
            assertEquals("Emergency stop card was partly hidden", emergency.height, visibleBounds.height())
        }
        val visualDirectory = java.io.File(context.cacheDir, "awero-visual").apply { mkdirs() }
        java.io.File(visualDirectory, "Math.png").outputStream().use { output ->
            checkNotNull(instrumentation.uiAutomation.takeScreenshot()).compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
        }

        if (android.os.Build.VERSION.SDK_INT >= 31) {
            val bytes = java.io.File(visualDirectory, "Math.png").readBytes()
            val descriptors = instrumentation.uiAutomation.executeShellCommandRw(
                "dd of=/data/local/tmp/awero-visual-Math.png"
            )
            android.os.ParcelFileDescriptor.AutoCloseOutputStream(descriptors[1]).use { it.write(bytes) }
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptors[0]).use { it.readBytes() }
            val saved = instrumentation.uiAutomation.executeShellCommand("cat /data/local/tmp/awero-visual-Math.png")
            val actual = android.os.ParcelFileDescriptor.AutoCloseInputStream(saved).use { it.readBytes() }
            check(bytes.contentEquals(actual)) { "Screenshot export did not preserve Math PNG bytes" }
        }

        instrumentation.runOnMainSync {
            val root = activity!!.window.decorView
            val pattern = Regex("(\\d+) ([+-]) (\\d+) = \\?")
            val problem = views(root).filterIsInstance<TextView>()
                .mapNotNull { pattern.matchEntire(it.text.toString()) }.single()
            val left = problem.groupValues[1].toInt()
            val right = problem.groupValues[3].toInt()
            val answer = if (problem.groupValues[2] == "+") left + right else left - right
            val input = views(root).filterIsInstance<EditText>().single()
            val keys = views(root).filterIsInstance<Button>()
            input.setText("9")
            keys.single { it.contentDescription == context.getString(R.string.mission_delete_digit) }.performClick()
            assertEquals("", input.text.toString())
            val sign = keys.single { it.contentDescription == context.getString(R.string.mission_change_sign) }
            sign.performClick()
            assertEquals("-", input.text.toString())
            sign.performClick()
            assertEquals("", input.text.toString())
            if (answer < 0) sign.performClick()
            kotlin.math.abs(answer).toString().forEach { digit ->
                keys.single { it.text.toString() == digit.toString() }.performClick()
            }
            assertEquals(answer.toString(), input.text.toString())
            views(root).filterIsInstance<TextView>().single { (it.text.toString() == context.getString(R.string.mission_check) || it.contentDescription == context.getString(R.string.mission_check)) }.performClick()
        }

        awaitCondition("Failed save did not offer a retry") {
            hasText(context.getString(R.string.wake_retry))
        }
        assertEquals(WakeFlowController.State.MISSION, flow.state.value)
        assertNotNull(testSessions.loadActive())

        instrumentation.runOnMainSync {
            views(activity!!.window.decorView).filterIsInstance<TextView>()
                .single { it.text.toString() == context.getString(R.string.wake_retry) }.performClick()
        }
        awaitCondition("Retry did not complete the wake session") {
            hasText(context.getString(R.string.wake_completed_title)) && runBlocking { testSessions.loadActive() == null }
        }
        assertEquals(2, completionAttempts)
        assertEquals("SUCCESS", testSessions.load(includeTestAlarms = true).single().result)
    }

    private fun hasText(expected: String): Boolean {
        var found = false
        instrumentation.runOnMainSync {
            activity?.let { current -> found = views(current.window.decorView).any { it is TextView && it.text.toString() == expected } }
        }
        return found
    }

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
