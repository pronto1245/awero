package app.awero.core.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.awero.core.alarm.Alarm
import app.awero.core.alarm.AlarmStore
import app.awero.core.alarm.Difficulty
import app.awero.core.alarm.MissionType
import app.awero.core.alarm.TimezoneMode
import app.awero.core.wake.WakeFlowController
import app.awero.core.wake.WakeSessionStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PersistenceRecoveryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun cleanup() {
        AweroDatabase.closeForTesting()
        context.deleteDatabase("awero.db")
    }

    @Test
    fun alarmSurvivesDatabaseReopen() = runBlocking {
        val db = AweroDatabase.get(context)
        val alarmId = UUID.randomUUID().toString()
        db.alarms().upsert(
            AlarmEntity(
                id = alarmId,
                version = 4,
                hour = 7,
                minute = 30,
                enabled = true,
                weekdays = "1,2,3,4,5",
                timezoneMode = "DEVICE_LOCAL",
                fixedTimezone = null,
                missionType = "MATH",
                difficulty = "MEDIUM",
                maxSnoozes = 3,
                snoozeMinutes = 10,
                qrExpectedCode = null
            )
        )

        AweroDatabase.closeForTesting()

        val reopened = AweroDatabase.get(context)
        val restored = reopened.alarms().get(alarmId)

        assertNotNull(restored)
        assertEquals(4, restored?.version)
        assertEquals(7, restored?.hour)
        assertEquals(30, restored?.minute)
    }

    @Test
    fun duplicateSyncOperationIdIsIgnored() = runBlocking {
        val db = AweroDatabase.get(context)
        val id = UUID.randomUUID().toString()
        val entity = SyncOperationEntity(
            id = id,
            operationType = "UPDATE_ALARM",
            entityType = "ALARM",
            entityId = UUID.randomUUID().toString(),
            clientVersion = 2,
            payload = "{}",
            occurredAt = 100,
            attempts = 0,
            nextAttemptAt = 0
        )

        db.syncOperations().insert(entity)
        db.syncOperations().insert(entity.copy(attempts = 9))

        val due = db.syncOperations().due(0)
        assertEquals(1, due.count { it.id == id })
        assertEquals(0, due.first { it.id == id }.attempts)
    }

    @Test
    fun wakeFlowRestoresMissionStateAfterProcessRestart() = runBlocking {
        val alarm = Alarm(
            id = UUID.randomUUID().toString(),
            version = 2,
            hour = 7,
            minute = 30,
            enabled = true,
            weekdays = setOf(1, 2, 3, 4, 5),
            timezoneMode = TimezoneMode.DEVICE_LOCAL,
            fixedTimezone = null,
            missionType = MissionType.MATH,
            difficulty = Difficulty.MEDIUM
        )
        AlarmStore(context).save(alarm)

        val first = WakeFlowController(WakeSessionStore(context), context)
        first.start(alarm, 1000)
        first.beginMission()

        AweroDatabase.closeForTesting()

        val restored = WakeFlowController(WakeSessionStore(context), context)
        restored.restore()

        assertEquals(WakeFlowController.State.MISSION, restored.state.value)
        assertEquals(MissionType.MATH, restored.mission.value)

        restored.beginMission()
        restored.completeMission()
        restored.completeMission()

        val db = AweroDatabase.get(context)
        val storedSession = db.wakeSessions().recent().first()
        val events = db.wakeEvents().forSession(storedSession.id)
        assertEquals(3, events.size)
        assertEquals(1, db.statistics().get()?.completed)

        AweroDatabase.closeForTesting()

        val finalRestore = WakeFlowController(WakeSessionStore(context), context)
        finalRestore.restore()
        assertEquals(WakeFlowController.State.IDLE, finalRestore.state.value)
    }

    @Test
    fun wakeEventAndSessionAreAtomicAndEventIdempotent() = runBlocking {
        val db = AweroDatabase.get(context)
        val sessionId = UUID.randomUUID().toString()
        val eventId = UUID.randomUUID().toString()
        val session = WakeSessionEntity(
            id = sessionId,
            alarmId = UUID.randomUUID().toString(),
            alarmVersion = 1,
            scheduledAt = 100,
            triggeredAt = 100,
            missionStartedAt = null,
            completedAt = null,
            result = null,
            snoozeCount = 0,
            fallbackUsed = false,
            emergencyStop = false
        )
        val event = WakeEventEntity(
            id = eventId,
            wakeSessionId = sessionId,
            eventType = "TRIGGERED",
            occurredAt = 100
        )

        db.wakeSessions().upsertWithEvent(session, event)
        db.wakeSessions().upsertWithEvent(session.copy(missionStartedAt = 200), event)

        val restored = db.wakeSessions().get(sessionId)
        val events = db.wakeEvents().forSession(sessionId)

        assertEquals(200L, restored?.missionStartedAt)
        assertEquals(1, events.size)
    }
}
