package app.awero.core.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.awero.core.storage.AweroDatabase
import app.awero.core.storage.SyncOperationEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncConflictStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "awero-sync-conflict-test.db"

    @After
    fun cleanup() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun recordsConflictAndRemovesOperationFromRetryQueue() = runBlocking {
        val database = AweroDatabase.createForTesting(context, databaseName)
        try {
            val operation = SyncOperationEntity(
                id = "op-1",
                operationType = "UPDATE_ALARM",
                entityType = "ALARM",
                entityId = "alarm-1",
                clientVersion = 1,
                payload = "{\"hour\":8}",
                occurredAt = 1_700_000_000_000L
            )
            database.syncOperations().insert(operation)

            SyncConflictStore(database).record(
                operation,
                SyncConflict("op-1", "VERSION_MISMATCH", 2, "{\"hour\":9}")
            )

            assertTrue(database.syncOperations().due(Long.MAX_VALUE).isEmpty())
            val conflict = database.syncConflicts().get("op-1")
            assertEquals("VERSION_MISMATCH", conflict?.code)
            assertEquals(2, conflict?.serverVersion)
            assertEquals("{\"hour\":8}", conflict?.localPayload)
            assertEquals("{\"hour\":9}", conflict?.serverEntityJson)
        } finally {
            database.close()
        }
    }
}
