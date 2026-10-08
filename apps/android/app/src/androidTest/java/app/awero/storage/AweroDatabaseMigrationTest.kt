package app.awero.storage

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.awero.core.storage.AweroDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class AweroDatabaseMigrationTest {
    @Test
    @Throws(IOException::class)
    fun migrate1To2PreservesOldDataAndCreatesWakeEvents() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "awero-migration-${System.nanoTime()}"
        val callback = object : SupportSQLiteOpenHelper.Callback(2) {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE alarms (id TEXT NOT NULL PRIMARY KEY, version INTEGER NOT NULL, hour INTEGER NOT NULL, minute INTEGER NOT NULL, enabled INTEGER NOT NULL, weekdays TEXT NOT NULL, timezoneMode TEXT NOT NULL, fixedTimezone TEXT, missionType TEXT NOT NULL, difficulty TEXT NOT NULL, maxSnoozes INTEGER NOT NULL, snoozeMinutes INTEGER NOT NULL, qrExpectedCode TEXT)")
                db.execSQL("CREATE TABLE wake_sessions (id TEXT NOT NULL PRIMARY KEY, alarmId TEXT NOT NULL, alarmVersion INTEGER NOT NULL, scheduledAt INTEGER NOT NULL, triggeredAt INTEGER, missionStartedAt INTEGER, completedAt INTEGER, result TEXT, snoozeCount INTEGER NOT NULL, fallbackUsed INTEGER NOT NULL, emergencyStop INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE statistics (id INTEGER NOT NULL PRIMARY KEY, planned INTEGER NOT NULL, completed INTEGER NOT NULL, snoozes INTEGER NOT NULL, fallback INTEGER NOT NULL, emergencyStops INTEGER NOT NULL, totalCompletionSeconds INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE sync_operations (id TEXT NOT NULL PRIMARY KEY, operationType TEXT NOT NULL, entityType TEXT NOT NULL, entityId TEXT NOT NULL, clientVersion INTEGER, payload TEXT NOT NULL, occurredAt INTEGER NOT NULL, attempts INTEGER NOT NULL, nextAttemptAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE analytics_events (id TEXT NOT NULL PRIMARY KEY, eventName TEXT NOT NULL, eventVersion INTEGER NOT NULL, payload TEXT NOT NULL, occurredAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE alarms_old_marker (id INTEGER NOT NULL PRIMARY KEY)")
                db.execSQL("DROP TABLE alarms_old_marker")
            }

            override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                if (oldVersion == 1 && newVersion >= 2) {
                    AweroDatabase.MIGRATION_1_2.migrate(db)
                }
            }
        }
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseName)
            .callback(callback)
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        val db = helper.writableDatabase
        db.execSQL("INSERT INTO alarms (id, version, hour, minute, enabled, weekdays, timezoneMode, fixedTimezone, missionType, difficulty, maxSnoozes, snoozeMinutes, qrExpectedCode) VALUES ('alarm-1', 1, 7, 30, 1, '1,2,3,4,5', 'DEVICE_LOCAL', NULL, 'MATH', 'MEDIUM', 3, 10, NULL)")
        db.execSQL("INSERT INTO wake_sessions (id, alarmId, alarmVersion, scheduledAt, triggeredAt, missionStartedAt, completedAt, result, snoozeCount, fallbackUsed, emergencyStop) VALUES ('session-1', 'alarm-1', 1, 1000, 1100, 1200, NULL, NULL, 1, 0, 0)")
        db.close()
        helper.close()
        val migratedHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(callback)
                .build()
        )
        val migrated = migratedHelper.writableDatabase

        val alarmCursor = migrated.query("SELECT hour, minute FROM alarms WHERE id = 'alarm-1'")
        alarmCursor.use {
            assertEquals(1, it.count)
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
