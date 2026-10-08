package app.awero.storage

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.awero.core.storage.AweroDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class AweroDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        ApplicationProvider.getApplicationContext<Context>(),
        AweroDatabase::class.java
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2PreservesOldDataAndCreatesWakeEvents() {
        val db = helper.createDatabase("awero-migration", 1)
        db.execSQL(
            "INSERT INTO alarms (id, version, hour, minute, enabled, weekdays, timezoneMode, fixedTimezone, missionType, difficulty, maxSnoozes, snoozeMinutes, qrExpectedCode) " +
                "VALUES ('alarm-1', 1, 7, 30, 1, '1,2,3,4,5', 'DEVICE_LOCAL', NULL, 'MATH', 'MEDIUM', 3, 10, NULL)"
        )
        db.execSQL(
            "INSERT INTO wake_sessions (id, alarmId, alarmVersion, scheduledAt, triggeredAt, missionStartedAt, completedAt, result, snoozeCount, fallbackUsed, emergencyStop) " +
                "VALUES ('session-1', 'alarm-1', 1, 1000, 1100, 1200, NULL, NULL, 1, 0, 0)"
        )
        db.close()

        val migrated = helper.runMigrationsAndValidate(
            "awero-migration",
            2,
            true,
            AweroDatabase.MIGRATION_1_2
        )

        val alarmCursor = migrated.query("SELECT hour, minute FROM alarms WHERE id = 'alarm-1'")
        alarmCursor.use {
            assertEquals(1, it.count)
            assertEquals(1, it.columnCount)
            it.moveToFirst()
            assertEquals(7, it.getInt(it.getColumnIndexOrThrow("hour")))
            assertEquals(30, it.getInt(it.getColumnIndexOrThrow("minute")))
        }

        val sessionCursor = migrated.query("SELECT id FROM wake_sessions WHERE id = 'session-1'")
        sessionCursor.use {
            assertEquals(1, it.count)
        }

        val eventCursor = migrated.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'wake_events'"
        )
        eventCursor.use {
            assertEquals(1, it.count)
        }

        val indexCursor = migrated.query(
            "SELECT name FROM sqlite_master WHERE type = 'index' AND name = 'index_wake_events_wakeSessionId'"
        )
        indexCursor.use {
            assertEquals(1, it.count)
        }

        migrated.close()
    }
}
