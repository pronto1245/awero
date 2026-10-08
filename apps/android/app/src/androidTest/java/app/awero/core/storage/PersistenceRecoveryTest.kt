package app.awero.core.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersistenceRecoveryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "awero-persistence-recovery.db"

    @After
    fun cleanup() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun syncOperationIsUniqueAndSurvivesDatabaseRestart() = runBlocking {
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
    fun wakeSessionTransitionIsIdempotentAcrossRestart() = runBlocking {
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
        val restoredDao = database.wakeSessions()
        val restored = restoredDao.get("session-1")
        assertNotNull(restored)
        assertEquals(2000L, restored?.missionStartedAt)
        assertEquals(1, restored?.snoozeCount)
        assertTrue(restored?.fallbackUsed == true)

        val completed = restored!!.copy(
            completedAt = 3000L,
            result = "SUCCESS"
        )
        restoredDao.upsert(completed)
        restoredDao.upsert(completed)

        assertNull(restoredDao.active())
        assertEquals("SUCCESS", restoredDao.get("session-1")?.result)
        database.close()
    }
}
