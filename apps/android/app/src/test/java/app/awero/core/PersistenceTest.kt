package app.awero.core

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import app.awero.core.alarm.Alarm
import app.awero.core.alarm.Difficulty
import app.awero.core.alarm.MissionType
import app.awero.core.storage.AweroDatabase
import app.awero.core.sync.SyncQueueStore
import app.awero.core.wake.WakeSessionStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PersistenceTest {
    private val context: Context
        get() = RuntimeEnvironment.getApplication()

    @Test
    fun wakeSessionSurvivesRestartAndTransitionsAreIdempotent() {
        val name = "wake-test.db"
        val alarm = Alarm(
            id = "alarm-1",
            version = 1,
            hour = 7,
            minute = 30,
            missionType = MissionType.MATH,
            difficulty = Difficulty.MEDIUM
        )

        val firstDb = AweroDatabase.createForTesting(context, name)
        val firstStore = WakeSessionStore(context, firstDb)
        assertEquals(true, firstStore.start(alarm, 1000L))
        assertEquals(false, firstStore.start(alarm, 1000L))
        firstStore.startMission()
        firstStore.startMission()
        firstDb.close()

        val secondDb = AweroDatabase.createForTesting(context, name)
        val secondStore = WakeSessionStore(context, secondDb)
        val restored = secondStore.loadActive()
        assertNotNull(restored)
        assertNotNull(restored?.missionStartedAt)

        val completed = secondStore.complete()
        assertNotNull(completed)
        assertNull(secondStore.complete())
        assertNull(secondStore.loadActive())

        val events = secondDb.wakeEvents().forSession(completed!!.id)
        assertEquals(listOf("TRIGGERED", "MISSION_STARTED", "COMPLETED"), events.map { it.eventType })

        secondDb.close()
        context.deleteDatabase(name)
    }

    @Test
    fun syncQueueIgnoresDuplicateIdsAndRetriesAfterRestart() {
        val name = "sync-test.db"
        val db = AweroDatabase.createForTesting(context, name)
        val queue = SyncQueueStore(context, db)
        val id = queue.enqueue(
            operationType = "UPDATE_ALARM",
            entityType = "ALARM",
            entityId = "alarm-1",
            payload = org.json.JSONObject().put("hour", 8),
            id = "operation-1"
        )
        queue.enqueue(
            operationType = "UPDATE_ALARM",
            entityType = "ALARM",
            entityId = "alarm-1",
            payload = org.json.JSONObject().put("hour", 8),
            id = id
        )
        assertEquals(1, queue.due(2000L).size)
        queue.retry(id, 1)
        assertEquals(0, queue.due(2000L).size)
        db.close()

        val reopened = AweroDatabase.createForTesting(context, name)
        val restoredQueue = SyncQueueStore(context, reopened)
        assertEquals(0, restoredQueue.due(2000L).size)
        assertEquals(1, restoredQueue.due(System.currentTimeMillis() + 10000L).size)
        restoredQueue.acknowledge(id)
        assertEquals(0, restoredQueue.due(System.currentTimeMillis() + 10000L).size)

        reopened.close()
        context.deleteDatabase(name)
    }

    @Test
    fun migratesRealVersionOneDatabaseToVersionTwo() {
        val name = "migration-test.db"
        createVersionOneDatabase(name)

        val migrated = AweroDatabase.createForTesting(context, name)
        val tables = migrated.openHelper.writableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='wake_events'"
        )
        assertEquals(true, tables.moveToFirst())
        tables.close()

        val columns = migrated.openHelper.writableDatabase.query("PRAGMA table_info(wake_events)")
        val names = buildList {
            while (columns.moveToNext()) add(columns.getString(1))
        }
        columns.close()
        assertEquals(listOf("id", "wakeSessionId", "eventType", "occurredAt", "payload"), names)

        migrated.close()
        context.deleteDatabase(name)
    }

    private fun createVersionOneDatabase(name: String) {
        val callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS alarms (id TEXT NOT NULL, version INTEGER NOT NULL, hour INTEGER NOT NULL, minute INTEGER NOT NULL, enabled INTEGER NOT NULL, weekdays TEXT NOT NULL, timezoneMode TEXT NOT NULL, fixedTimezone TEXT, missionType TEXT NOT NULL, difficulty TEXT NOT NULL, maxSnoozes INTEGER NOT NULL, snoozeMinutes INTEGER NOT NULL, qrExpectedCode TEXT, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS wake_sessions (id TEXT NOT NULL, alarmId TEXT NOT NULL, alarmVersion INTEGER NOT NULL, scheduledAt INTEGER NOT NULL, triggeredAt INTEGER, missionStartedAt INTEGER, completedAt INTEGER, result TEXT, snoozeCount INTEGER NOT NULL, fallbackUsed INTEGER NOT NULL, emergencyStop INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS statistics (id INTEGER NOT NULL, planned INTEGER NOT NULL, completed INTEGER NOT NULL, snoozes INTEGER NOT NULL, fallback INTEGER NOT NULL, emergencyStops INTEGER NOT NULL, totalCompletionSeconds INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS sync_operations (id TEXT NOT NULL, operationType TEXT NOT NULL, entityType TEXT NOT NULL, entityId TEXT NOT NULL, clientVersion INTEGER, payload TEXT NOT NULL, occurredAt INTEGER NOT NULL, attempts INTEGER NOT NULL, nextAttemptAt INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS analytics_events (id TEXT NOT NULL, eventName TEXT NOT NULL, eventVersion INTEGER NOT NULL, payload TEXT NOT NULL, occurredAt INTEGER NOT NULL, PRIMARY KEY(id))")
            }

            override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(callback)
                .build()
        )
        helper.writableDatabase.close()
    }
}
