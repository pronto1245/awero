package app.awero.core

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import app.awero.core.alarm.Alarm
import app.awero.core.alarm.Difficulty
import app.awero.core.alarm.MissionType
import app.awero.core.storage.AweroDatabase
import app.awero.core.storage.StatisticsEntity
import app.awero.core.statistics.StatisticsStore
import app.awero.core.wake.WakeFlowController
import app.awero.core.sync.SyncQueueStore
import app.awero.core.wake.WakeSessionStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PersistenceTest {
    private val context: Context
        get() = RuntimeEnvironment.getApplication()

    @Test
    fun missingStatisticsMigrationMarkerDoesNotOverwriteCommittedCounters() = runBlocking {
        val name = "statistics-migration-restart.db"
        val preferences = context.getSharedPreferences("awero_statistics", Context.MODE_PRIVATE)
        preferences.edit()
            .putString("statistics", """{"planned":99,"completed":88}""")
            .putBoolean("room_migrated", false)
            .commit()
        var database = AweroDatabase.createForTesting(context, name)
        database.statistics().upsert(StatisticsEntity(planned = 4, completed = 2))
        database.close()

        database = AweroDatabase.createForTesting(context, name)
        StatisticsStore(context, database).recordPlanned()
        assertEquals(5, database.statistics().get()!!.planned)
        assertEquals(2, database.statistics().get()!!.completed)
        database.close()

        preferences.edit().putBoolean("room_migrated", false).commit()
        database = AweroDatabase.createForTesting(context, name)
        try {
            StatisticsStore(context, database).recordPlanned()
            assertEquals(6, database.statistics().get()!!.planned)
            assertEquals(2, database.statistics().get()!!.completed)
        } finally {
            database.close()
            context.deleteDatabase(name)
            preferences.edit().clear().commit()
        }
        Unit
    }

    @Test
    fun alarmSurvivesRestart() = runBlocking {
        val name = "alarm-test.db"
        val alarm = Alarm(
            id = "alarm-persisted",
            version = 3,
            hour = 7,
            minute = 45,
            enabled = true,
            weekdays = setOf(1, 2, 3, 4, 5),
            missionType = MissionType.QR,
            difficulty = Difficulty.HARD,
            qrExpectedCode = "awero://wake/bedroom?code=one+two",
            label = "Weekday"
        )

        val firstDb = AweroDatabase.createForTesting(context, name)
        val firstStore = app.awero.core.alarm.AlarmStore(context, firstDb)
        firstStore.save(alarm)
        firstDb.close()

        val secondDb = AweroDatabase.createForTesting(context, name)
        val secondStore = app.awero.core.alarm.AlarmStore(context, secondDb)
        val restored = secondStore.get(alarm.id)
        assertEquals(alarm, restored)

        secondDb.close()
        context.deleteDatabase(name)
        Unit
    }

    @Test
    fun fullWakeFlowRestoresUiAndPersistsStatistics() = runBlocking {
        val name = "e2e-test.db"
        val db = AweroDatabase.createForTesting(context, name)
        val alarm = Alarm(
            id = "e2e-alarm",
            version = 1,
            hour = 6,
            minute = 30,
            enabled = true,
            missionType = MissionType.MATH,
            difficulty = Difficulty.EASY
        )
        val alarmStore = app.awero.core.alarm.AlarmStore(context, db)
        kotlinx.coroutines.runBlocking { alarmStore.save(alarm) }
        val sessions1 = WakeSessionStore(context, db)
        val stats = StatisticsStore(context, db)
        val controller1 = WakeFlowController(sessions1, context, stats, alarmStore = alarmStore)

        kotlinx.coroutines.runBlocking {
            controller1.start(alarm, 1000L)
            controller1.beginMission()
        }
        assertEquals(WakeFlowController.State.MISSION, controller1.state.value)

        val controller2 = WakeFlowController(
            WakeSessionStore(context, db),
            context,
            StatisticsStore(context, db),
            alarmStore = alarmStore
        )
        kotlinx.coroutines.runBlocking { controller2.restore() }
        assertEquals(WakeFlowController.State.MISSION, controller2.state.value)
        assertEquals(MissionType.MATH, controller2.mission.value)

        kotlinx.coroutines.runBlocking { controller2.completeMission() }
        assertEquals(WakeFlowController.State.COMPLETED, controller2.state.value)

        val restoredStats = db.statistics().get()
        assertNotNull(restoredStats)
        assertEquals(1, restoredStats?.planned)
        assertEquals(1, restoredStats?.completed)

        db.close()
        context.deleteDatabase(name)
        Unit
    }

    @Test
    fun wakeSessionSurvivesRestartAndTransitionsAreIdempotent() = runBlocking {
        val name = "wake-test.db"
        val alarm = Alarm(
            id = "alarm-1",
            version = 1,
            hour = 7,
            enabled = true,
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
        Unit
    }

    @Test
    fun syncQueueIgnoresDuplicateIdsAndRetriesAfterRestart() = runBlocking {
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
        Unit
    }

    @Test
    fun migratesRealVersionOneDatabaseToVersionTwo() = runBlocking {
        val name = "migration-test.db"
        val helper = createVersionOneDatabase(name)
        val database = helper.writableDatabase

        AweroDatabase.MIGRATION_1_2.migrate(database)

        val tables = database.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='wake_events'"
        )
        assertEquals(true, tables.moveToFirst())
        tables.close()

        val columns = database.query("PRAGMA table_info(wake_events)")
        val names = buildList {
            while (columns.moveToNext()) add(columns.getString(1))
        }
        columns.close()
        assertEquals(listOf("id", "wakeSessionId", "eventType", "occurredAt", "payload"), names)

        helper.close()
        context.deleteDatabase(name)
        Unit
    }

    private fun createVersionOneDatabase(name: String): SupportSQLiteOpenHelper {
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
        return FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(callback)
                .build()
        )
    }


}
