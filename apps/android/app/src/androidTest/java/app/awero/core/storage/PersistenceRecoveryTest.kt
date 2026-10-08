package app.awero.core.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistenceRecoveryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "awero-persistence-recovery.db"

    @After
    fun cleanup() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun syncOperationIsUniqueAndSurvivesDatabaseRestart() {
        var database = AweroDatabase.createForTesting(context, databaseName)
        val dao = database.syncOperations()
        val operation = SyncOperationEntity(
            id = "sync-1",
            operationType = "UPDATE_ALARM",
            entityType = "ALARM",
            entityId = "alarm-1",
            clientVersion = 2,
            payload = """{"hour":8}""",
            occurredAt = 1000L
        )

        dao.insert(operation)
        dao.insert(operation)
        assertEquals(1, dao.due(1000L).size)

        dao.retry("sync-1", 5000L)
        database.close()

        database = AweroDatabase.createForTesting(context, databaseName)
        val restored = database.syncOperations().due(5000L)
        assertEquals(1, restored.size)
        assertEquals(1, restored.single().attempts)
        assertEquals(5000L, restored.single().nextAttemptAt)
        database.close()
    }

    @Test
    fun wakeSessionTransitionIsIdempotentAcrossRestart() {
        var database = AweroDatabase.createForTesting(context, databaseName)
        val dao = database.wakeSessions()
        val session = WakeSessionEntity(
            id = "session-1",
            alarmId = "alarm-1",
            alarmVersion = 1,
            scheduledAt = 1000L,
            triggeredAt = 1000L,
            missionStartedAt = 2000L,
            completedAt = null,
            result = null,
            snoozeCount = 1,
            fallbackUsed = true,
            emergencyStop = false
        )
        dao.upsert(session)
        dao.upsert(session)
        assertNotNull(dao.active())
        database.close()

        database = AweroDatabase.createForTesting(context, databaseName)
        val restored = dao.get("session-1")
        assertNotNull(restored)
        assertEquals(2000L, restored?.missionStartedAt)
        assertEquals(1, restored?.snoozeCount)
        assertTrue(restored?.fallbackUsed == true)

        val completed = restored!!.copy(
            completedAt = 3000L,
            result = "SUCCESS"
        )
        dao.upsert(completed)
        dao.upsert(completed)

        assertNull(dao.active())
        assertEquals("SUCCESS", dao.get("session-1")?.result)
        database.close()
    }
}
