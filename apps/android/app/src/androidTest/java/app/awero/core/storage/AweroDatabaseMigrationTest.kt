package app.awero.core.storage

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AweroDatabaseMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "awero-migration-test.db"

    @After
    fun cleanup() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrationFromVersion1CreatesWakeEventsTable() {
        context.deleteDatabase(databaseName)

        val database = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        database.execSQL("CREATE TABLE alarms (id TEXT NOT NULL PRIMARY KEY, version INTEGER NOT NULL, hour INTEGER NOT NULL, minute INTEGER NOT NULL, enabled INTEGER NOT NULL, weekdays TEXT NOT NULL, timezoneMode TEXT NOT NULL, fixedTimezone TEXT, missionType TEXT NOT NULL, difficulty TEXT NOT NULL, maxSnoozes INTEGER NOT NULL, snoozeMinutes INTEGER NOT NULL, qrExpectedCode TEXT)")
        database.execSQL("CREATE TABLE wake_sessions (id TEXT NOT NULL PRIMARY KEY, alarmId TEXT NOT NULL, alarmVersion INTEGER NOT NULL, scheduledAt INTEGER NOT NULL, triggeredAt INTEGER, missionStartedAt INTEGER, completedAt INTEGER, result TEXT, snoozeCount INTEGER NOT NULL, fallbackUsed INTEGER NOT NULL, emergencyStop INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE statistics (id INTEGER NOT NULL PRIMARY KEY, planned INTEGER NOT NULL, completed INTEGER NOT NULL, snoozes INTEGER NOT NULL, fallback INTEGER NOT NULL, emergencyStops INTEGER NOT NULL, totalCompletionSeconds INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE sync_operations (id TEXT NOT NULL PRIMARY KEY, operationType TEXT NOT NULL, entityType TEXT NOT NULL, entityId TEXT NOT NULL, clientVersion INTEGER, payload TEXT NOT NULL, occurredAt INTEGER NOT NULL, attempts INTEGER NOT NULL, nextAttemptAt INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE analytics_events (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, version INTEGER NOT NULL, payload TEXT NOT NULL, occurredAt INTEGER NOT NULL)")
        database.execSQL("PRAGMA user_version = 1")
        database.close()

        val migrated = Room.databaseBuilder(
            context,
            AweroDatabase::class.java,
            databaseName
        ).addMigrations(AweroDatabase.MIGRATION_1_2).build()

        migrated.openHelper.writableDatabase
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
        assertTrue(db.query(
            "SELECT payload FROM wake_events WHERE id='event-1'"
        ).use { it.moveToFirst() })

        migrated.close()
    }
}
