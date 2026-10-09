package app.awero.core.wake

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.awero.core.alarm.Alarm
import app.awero.core.alarm.AlarmStore
import app.awero.core.alarm.Difficulty
import app.awero.core.alarm.MissionType
import app.awero.core.storage.AweroDatabase
import app.awero.core.statistics.StatisticsStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WakeFlowControllerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "awero-wake-flow.db"
    private val database = AweroDatabase.createForTesting(context, databaseName)

    @After
    fun cleanup() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun distinctAlarmTriggerStartsAnIndependentWakeSession() = runBlocking {
        val store = AlarmStore(context, database)
        val sessions = WakeSessionStore(context, database)
        val stats = StatisticsStore(context, database)
        val first = alarm("first-alarm")
        val second = alarm("second-alarm")
        store.save(first)
        store.save(second)

        val flow = WakeFlowController(sessions, context, stats, alarmStore = store)
        flow.start(first, 1_000L)
        flow.beginMission()
        flow.start(second, 2_000L)

        assertEquals(WakeFlowController.State.RINGING, flow.state.value)
        assertEquals(second.id, flow.currentAlarm.value?.id)
        assertEquals(MissionType.MATH, flow.mission.value)
        assertEquals(2, stats.statistics().planned)
        assertEquals(first.id, sessions.loadActive(first.id, false)?.alarmId)
        assertTrue(sessions.loadActive(first.id, false)?.missionStartedAt != null)
        assertEquals(second.id, sessions.loadActive(second.id, false)?.alarmId)
    }

    @Test
    fun testAlarmCompletionDoesNotChangeWakeStatistics() = runBlocking {
        val store = AlarmStore(context, database)
        val sessions = WakeSessionStore(context, database)
        val stats = StatisticsStore(context, database)
        val alarm = alarm("test-alarm-history")
        store.save(alarm)
        val flow = WakeFlowController(sessions, context, stats, testAlarm = true, alarmStore = store)

        flow.start(alarm, 1_000L)
        assertTrue(flow.beginMission())
        assertTrue(flow.completeMission())

        assertTrue(sessions.load().isEmpty())
        val storedTest = database.wakeSessions().recent(includeTest = true).single()
        assertEquals("SUCCESS", storedTest.result)
        assertTrue(storedTest.isTest)
        assertEquals(0, stats.statistics().planned)
        assertEquals(0, stats.statistics().completed)
    }

    @Test
    fun terminalUiStateRequiresAPersistedSession() = runBlocking {
        val store = AlarmStore(context, database)
        val sessions = WakeSessionStore(context, database)
        val stats = StatisticsStore(context, database)
        val alarm = alarm("terminal-state")
        store.save(alarm)

        val flow = WakeFlowController(sessions, context, stats, alarmStore = store)
        flow.start(alarm, 1_000L)
        assertTrue(flow.beginMission())
        assertTrue(sessions.complete() != null)

        assertFalse(flow.completeMission())
        assertEquals(WakeFlowController.State.MISSION, flow.state.value)
        assertFalse(flow.emergencyStop())
        assertEquals(WakeFlowController.State.MISSION, flow.state.value)
    }

    @Test
    fun emergencyStopWithoutASessionDoesNotChangeState() = runBlocking {
        val flow = WakeFlowController(
            sessions = WakeSessionStore(context, database),
            context = context,
            statistics = StatisticsStore(context, database)
        )

        assertFalse(flow.emergencyStop())
        assertEquals(WakeFlowController.State.IDLE, flow.state.value)
    }

    @Test
    fun failedSnoozeLeavesAlarmRingingAndDoesNotConsumeAllowance() = runBlocking {
        val store = AlarmStore(context, database)
        val sessions = WakeSessionStore(context, database)
        val stats = StatisticsStore(context, database)
        val alarm = alarm("snooze-alarm", maxSnoozes = 2)
        store.save(alarm)
        var cancelCount = 0
        val flow = WakeFlowController(
            sessions = sessions,
            context = context,
            statistics = stats,
            alarmStore = store,
            scheduleAlarmSnooze = { _, _, _ -> throw SecurityException("Exact alarm access denied") },
            cancelAlarmSnooze = { _, _ -> cancelCount += 1 }
        )
        flow.start(alarm, 1_000L)

        val scheduled = flow.snooze()

        assertFalse(scheduled)
        assertEquals(WakeFlowController.State.RINGING, flow.state.value)
        assertEquals(0, flow.snoozeCount.value)
        assertEquals(1, cancelCount)
        assertTrue(flow.snoozeError.value.orEmpty().contains("Exact alarm access denied"))
        assertEquals(0, sessions.loadActive()?.snoozeCount)
    }

    @Test
    fun successfulSnoozePersistsBeforeLeavingRingingState() = runBlocking {
        val store = AlarmStore(context, database)
        val sessions = WakeSessionStore(context, database)
        val stats = StatisticsStore(context, database)
        val alarm = alarm("successful-snooze", maxSnoozes = 2)
        store.save(alarm)
        val flow = WakeFlowController(
            sessions = sessions,
            context = context,
            statistics = stats,
            alarmStore = store,
            scheduleAlarmSnooze = { _, _, _ -> },
            cancelAlarmSnooze = { _, _ -> }
        )
        flow.start(alarm, 1_000L)

        assertTrue(flow.snooze())
        assertEquals(WakeFlowController.State.IDLE, flow.state.value)
        assertEquals(1, flow.snoozeCount.value)
        assertEquals(1, sessions.loadActive()?.snoozeCount)

        val restored = WakeFlowController(sessions, context, stats, alarmStore = store)
        restored.restore(alarm.id, testAlarm = false)
        assertEquals(WakeFlowController.State.RINGING, restored.state.value)
        assertEquals(1, restored.snoozeCount.value)
    }

    @Test
    fun repeatedSnoozeDuringPersistenceDoesNotCancelScheduledAlarm() = runBlocking {
        val store = AlarmStore(context, database)
        val sessions = WakeSessionStore(context, database)
        val stats = StatisticsStore(context, database)
        val alarm = alarm("overlapping-snooze", maxSnoozes = 2)
        store.save(alarm)
        var schedules = 0
        var cancellations = 0
        val flow = WakeFlowController(
            sessions, context, stats, alarmStore = store,
            scheduleAlarmSnooze = { _, _, _ -> schedules += 1 },
            cancelAlarmSnooze = { _, _ -> cancellations += 1 }
        )
        flow.start(alarm, 1_000L)

        // Run until the Room write suspends, then deliver the second tap.
        val first = async(start = CoroutineStart.UNDISPATCHED) { flow.snooze() }
        val repeated = flow.snooze()
        assertFalse(repeated)
        assertTrue(first.await())
        assertEquals(1, schedules)
        assertEquals(0, cancellations)
        assertEquals(1, sessions.loadActive()?.snoozeCount)

        flow.start(alarm, 2_000L)
        assertTrue(flow.snooze())
        assertEquals(2, schedules)
        assertEquals(0, cancellations)
        assertEquals(2, sessions.loadActive()?.snoozeCount)
    }

    @Test
    fun simultaneousAlarmsKeepIndependentActiveWakeSessions() = runBlocking {
        val alarms = AlarmStore(context, database)
        val first = alarm("concurrent-first")
        val second = alarm("concurrent-second")
        alarms.save(first)
        alarms.save(second)

        val firstSessions = WakeSessionStore(context, database)
        val secondSessions = WakeSessionStore(context, database)
        val statistics = StatisticsStore(context, database)
        val firstFlow = WakeFlowController(firstSessions, context, statistics, alarmStore = alarms)
        val secondFlow = WakeFlowController(secondSessions, context, statistics, alarmStore = alarms)
        firstFlow.start(first, 1_000L)
        secondFlow.start(second, 2_000L)

        assertEquals(WakeFlowController.State.RINGING, firstFlow.state.value)
        assertEquals(WakeFlowController.State.RINGING, secondFlow.state.value)
        assertEquals(first.id, firstSessions.loadActive(first.id, false)?.alarmId)
        assertEquals(second.id, secondSessions.loadActive(second.id, false)?.alarmId)
        assertEquals(2, firstSessions.load().size)
        assertEquals(2, statistics.statistics().planned)

        assertTrue(secondFlow.beginMission())
        assertEquals(WakeFlowController.State.RINGING, firstFlow.state.value)
        assertEquals(WakeFlowController.State.MISSION, secondFlow.state.value)
        assertEquals(first.id, firstSessions.loadActive(first.id, false)?.alarmId)
        assertEquals(second.id, secondSessions.loadActive(second.id, false)?.alarmId)
    }

    @Test
    fun simultaneousTestAndRealWakeForSameAlarmStaySeparate() = runBlocking {
        val alarms = AlarmStore(context, database)
        val alarm = alarm("real-and-test")
        alarms.save(alarm)
        val statistics = StatisticsStore(context, database)
        val realSessions = WakeSessionStore(context, database)
        val testSessions = WakeSessionStore(context, database)
        val realFlow = WakeFlowController(realSessions, context, statistics, alarmStore = alarms)
        val testFlow = WakeFlowController(testSessions, context, statistics, testAlarm = true, alarmStore = alarms)

        realFlow.start(alarm, 1_000L)
        testFlow.start(alarm, 1_000L)

        assertEquals(alarm.id, realSessions.loadActive(alarm.id, false)?.alarmId)
        assertEquals(alarm.id, testSessions.loadActive(alarm.id, true)?.alarmId)
        assertEquals(1, realSessions.load().size)
        assertEquals(1, statistics.statistics().planned)
    }

    @Test
    fun concurrentDuplicateTriggerCreatesOnlyOneSession() = runBlocking {
        val alarm = alarm("racing-trigger")
        val firstStore = WakeSessionStore(context, database)
        val secondStore = WakeSessionStore(context, database)

        val first = async { firstStore.start(alarm, 1_000L) }
        val second = async { secondStore.start(alarm, 1_000L) }

        assertEquals(1, listOf(first.await(), second.await()).count { it })
        val firstSession = firstStore.loadActive(alarm.id, false)
        val secondSession = secondStore.loadActive(alarm.id, false)
        assertEquals(firstSession?.id, secondSession?.id)
        assertEquals(1, firstStore.load().size)
    }

    private fun alarm(id: String, maxSnoozes: Int = 3) = Alarm(
        id = id,
        version = 1,
        hour = 7,
        minute = 30,
        enabled = true,
        missionType = MissionType.MATH,
        difficulty = Difficulty.EASY,
        maxSnoozes = maxSnoozes
    )
}
