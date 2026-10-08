package app.awero.core.storage

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.awero.core.alarm.Alarm
import app.awero.core.alarm.AlarmCoordinator
import app.awero.core.alarm.AlarmScheduler
import app.awero.core.alarm.AlarmStore
import app.awero.core.alarm.Difficulty
import app.awero.core.alarm.MissionType
import app.awero.core.statistics.StatisticsStore
import app.awero.core.sync.SyncQueueStore
import app.awero.core.wake.WakeFlowController
import app.awero.core.wake.WakeSessionStore
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProcessCrashRecoveryTest {
    @Test
    fun persistenceAcrossSigkill(): Unit = runBlocking {
        val phase = InstrumentationRegistry.getArguments().getString("aweroCrashPhase")
        assumeNotNull(phase)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val marker = File(context.filesDir, "awero-crash-ready")
        val name = "awero-process-crash.db"
        if (phase == "write") {
            marker.delete()
            context.deleteDatabase(name)
        }
        val database = AweroDatabase.createForTesting(context, name)
        val alarms = AlarmStore(context, database)
        val sessions = WakeSessionStore(context, database)
        val statistics = StatisticsStore(context, database)
        val queue = SyncQueueStore(context, database)
        val scheduler = AlarmScheduler(context)
        val controller = WakeFlowController(sessions, context, statistics, alarmStore = alarms)

        when (phase) {
            "write" -> {
                val alarm = Alarm(
                    id = "process-crash-alarm", version = 1, hour = 7, minute = 30,
                    enabled = true, missionType = MissionType.STEPS, difficulty = Difficulty.MEDIUM
                )
                alarms.save(alarm)
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                ParcelFileDescriptor.AutoCloseInputStream(
                    instrumentation.uiAutomation.executeShellCommand(
                        "appops set ${context.packageName} SCHEDULE_EXACT_ALARM allow"
                    )
                ).bufferedReader().use { it.readText() }
                assertTrue(
                    "Exact alarm app-op must be granted in the test fixture",
                    context.getSystemService(android.app.AlarmManager::class.java).canScheduleExactAlarms()
                )
                scheduler.schedule(alarm)
                assertTrue(scheduler.isScheduled(alarm))
                controller.start(alarm, 1000L)
                controller.beginMission()
                sessions.setSnoozeCount(1)
                controller.fallbackToMath()
                val operationId = queue.enqueue(
                    "UPDATE_ALARM", "ALARM", alarm.id, JSONObject().put("hour", 8),
                    clientVersion = 1, id = "process-crash-operation"
                )
                queue.enqueue("UPDATE_ALARM", "ALARM", alarm.id, JSONObject(), id = operationId)
                database.syncOperations().retry(operationId, System.currentTimeMillis() + 3_600_000L)
                assertEquals(1, queue.due(Long.MAX_VALUE).size)
                assertEquals(1, database.statistics().get()!!.planned)
                marker.writeText(sessions.loadActive()!!.id)
                // CI sends SIGKILL while the Room database and process are still open.
                while (true) delay(1000)
            }
            "read" -> {
                val alarm = alarms.get("process-crash-alarm")!!
                assertEquals(7, alarm.hour)
                assertEquals(30, alarm.minute)
                val restored = sessions.loadActive()!!
                assertEquals(marker.readText(), restored.id)
                assertNotNull(restored.missionStartedAt)
                assertEquals(1, restored.snoozeCount)
                assertTrue(restored.fallbackUsed)
                assertFalse(sessions.start(alarm, 1000L))
                assertEquals(0, queue.due().size)
                val pending = queue.due(Long.MAX_VALUE).single()
                assertEquals(1, pending.attempts)
                queue.enqueue("UPDATE_ALARM", "ALARM", alarm.id, JSONObject(), id = pending.id)
                val unique = queue.due(Long.MAX_VALUE).single()
                assertEquals(pending, unique)
                assertEquals(0, queue.due().size)
                queue.acknowledge(pending.id)
                assertTrue(queue.due(Long.MAX_VALUE).isEmpty())

                assertTrue(scheduler.isScheduled(alarm))
                scheduler.cancel(alarm)
                assertFalse(scheduler.isScheduled(alarm))
                val disabled = alarm.copy(id = "process-crash-disabled", enabled = false)
                alarms.save(disabled)
                scheduler.schedule(disabled.copy(enabled = true))
                assertTrue(scheduler.isScheduled(disabled.copy(enabled = true)))
                AlarmCoordinator(context, alarms, scheduler).repair()
                assertTrue(scheduler.isScheduled(alarm))
                assertFalse(scheduler.isScheduled(disabled.copy(enabled = true)))
                val dump = ParcelFileDescriptor.AutoCloseInputStream(
                    InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("dumpsys alarm")
                ).bufferedReader().use { it.readText() }
                assertTrue("Repaired alarm missing from AlarmManager", dump.contains(AlarmScheduler.ACTION_ALARM))

                controller.restore()
                assertEquals(WakeFlowController.State.MISSION, controller.state.value)
                assertEquals(MissionType.MATH, controller.mission.value)
                assertEquals(1, controller.snoozeCount.value)
                controller.completeMission()
                controller.completeMission()
                assertEquals(WakeFlowController.State.COMPLETED, controller.state.value)
                assertNull(sessions.complete())
                assertNull(sessions.loadActive())
                database.close()

                val reopened = AweroDatabase.createForTesting(context, name)
                try {
                    val totals = reopened.statistics().get()!!
                    assertEquals(1, totals.planned)
                    assertEquals(1, totals.completed)
                    assertEquals(1, totals.snoozes)
                    assertEquals(1, totals.fallback)
                    assertNull(reopened.wakeSessions().active())
                    assertEquals("SUCCESS", reopened.wakeSessions().get(restored.id)!!.result)
                    assertEquals(
                        listOf("TRIGGERED", "MISSION_STARTED", "SNOOZE", "FALLBACK", "COMPLETED"),
                        reopened.wakeEvents().forSession(restored.id).map { it.eventType }
                    )
                } finally {
                    reopened.close()
                    scheduler.cancel(alarm)
                    context.deleteDatabase(name)
                    marker.delete()
                }
            }
            else -> error("Unknown crash phase: $phase")
        }
        Unit
    }
}
