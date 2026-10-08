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
    fun duplicateTriggerRestoresTheExistingSession() = runBlocking {
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

        assertEquals(WakeFlowController.State.MISSION, flow.state.value)
        assertEquals(first.id, flow.currentAlarm.value?.id)
        assertEquals(MissionType.MATH, flow.mission.value)
        assertEquals(1, stats.statistics().planned)
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
            scheduleAlarmSnooze = { _, _ -> throw SecurityException("Exact alarm access denied") },
            cancelAlarmSnooze = { cancelCount += 1 }
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
            scheduleAlarmSnooze = { _, _ -> },
            cancelAlarmSnooze = {}
        )
        flow.start(alarm, 1_000L)

        assertTrue(flow.snooze())
        assertEquals(WakeFlowController.State.IDLE, flow.state.value)
        assertEquals(1, flow.snoozeCount.value)
        assertEquals(1, sessions.loadActive()?.snoozeCount)
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
            scheduleAlarmSnooze = { _, _ -> schedules += 1 },
            cancelAlarmSnooze = { cancellations += 1 }
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
