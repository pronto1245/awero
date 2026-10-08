package app.awero.core.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AweroDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "awero-migration-test.db"

    @After
    fun cleanup() {
        LegacyAweroDatabase.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrationFromVersion1CreatesWakeEventsTable() {
        val legacy = Room.databaseBuilder(
            context,
            LegacyAweroDatabase::class.java,
            databaseName
        ).build()
        legacy.openHelper.writableDatabase
        legacy.close()

        val migrated = AweroDatabase.createForTesting(context, databaseName)
        val db = migrated.openHelper.writableDatabase

        assertTrue(db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='wake_events'"
        ).use { it.moveToFirst() })
        assertTrue(db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND name='index_wake_events_wakeSessionId'"
        ).use { it.moveToFirst() })

        db.execSQL(
            "INSERT INTO wake_events(id, wakeSessionId, eventType, occurredAt, payload) VALUES(?,?,?,?,?)",
            arrayOf("event-1", "session-1", "TRIGGERED", 123L, "{}")
        )
        val payload = db.query(
            "SELECT payload FROM wake_events WHERE id='event-1'"
        ).use {
            assertTrue(it.moveToFirst())
            it.getString(0)
        }
        assertEquals("{}", payload)
        migrated.close()
    }
}

@Database(
    entities = [
        AlarmEntity::class,
        WakeSessionEntity::class,
        StatisticsEntity::class,
        SyncOperationEntity::class,
        AnalyticsEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LegacyAweroDatabase : RoomDatabase() {
    companion object {
        fun close() {}
    }
}
