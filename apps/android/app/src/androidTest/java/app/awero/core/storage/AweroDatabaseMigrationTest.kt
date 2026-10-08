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
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrationFromVersion1PreservesDataAndCreatesWakeEvents() {
        val legacy = Room.databaseBuilder(context, LegacyAweroDatabase::class.java, databaseName).build()
        val db = legacy.openHelper.writableDatabase
        db.execSQL("INSERT INTO alarms (id, version, hour, minute, enabled, weekdays, timezoneMode, fixedTimezone, missionType, difficulty, maxSnoozes, snoozeMinutes, qrExpectedCode) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)", arrayOf("alarm-1", 1, 7, 30, 1, "1,2,3,4,5", "DEVICE_LOCAL", null, "MATH", "MEDIUM", 3, 10, null))
        db.execSQL("INSERT INTO wake_sessions (id, alarmId, alarmVersion, scheduledAt, triggeredAt, missionStartedAt, completedAt, result, missionType, completionTimeSeconds, snoozeCount, fallbackUsed, emergencyStop) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)", arrayOf("session-1", "alarm-1", 1, 1000L, 1100L, null, null, null, "MATH", null, 0, 0, 0))
        legacy.close()

        val migrated = AweroDatabase.createForTesting(context, databaseName)
        val migratedDb = migrated.openHelper.writableDatabase

        val alarm = migratedDb.query("SELECT hour, minute FROM alarms WHERE id='alarm-1'").use {
            assertTrue(it.moveToFirst())
            it.getInt(0) to it.getInt(1)
        }
        assertEquals(7, alarm.first)
        assertEquals(30, alarm.second)

        assertTrue(migratedDb.query("SELECT id FROM wake_sessions WHERE id='session-1'").use { it.moveToFirst() })
        assertTrue(migratedDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='wake_events'").use { it.moveToFirst() })
        assertTrue(migratedDb.query("SELECT name FROM sqlite_master WHERE type='index' AND name='index_wake_events_wakeSessionId'").use { it.moveToFirst() })

        migratedDb.execSQL("INSERT INTO wake_events(id, wakeSessionId, eventType, occurredAt, payload) VALUES(?,?,?,?,?)", arrayOf("event-1", "session-1", "TRIGGERED", 123L, "{}"))
        assertEquals("{}", migratedDb.query("SELECT payload FROM wake_events WHERE id='event-1'").use { it.moveToFirst(); it.getString(0) })
        migrated.close()
    }
}

@Database(
    entities = [AlarmEntity::class, WakeSessionEntity::class, StatisticsEntity::class, SyncOperationEntity::class, AnalyticsEventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LegacyAweroDatabase : RoomDatabase()
