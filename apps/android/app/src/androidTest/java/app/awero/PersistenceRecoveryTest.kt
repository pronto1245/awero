package app.awero

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.awero.core.alarm.Alarm
import app.awero.core.alarm.Difficulty
import app.awero.core.alarm.MissionType
import app.awero.core.alarm.TimezoneMode
import app.awero.core.storage.AlarmEntity
import app.awero.core.storage.AnalyticsEventEntity
import app.awero.core.storage.AweroDatabase
import app.awero.core.storage.StatisticsEntity
import app.awero.core.storage.SyncOperationEntity
import app.awero.core.storage.WakeSessionEntity
import app.awero.core.wake.WakeSessionStore
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersistenceRecoveryTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun wakeSession_survives_close_and_reopen_and_transitions_are_idempotent() = runBlocking {
        val name = "awero-restart-${System.nanoTime()}.db"
        val alarm = Alarm(
            id = "alarm-restart",
            version = 1,
            hour = 7,
            minute = 30,
            enabled = true,
            weekdays = setOf(1, 2, 3, 4, 5),
            timezoneMode = TimezoneMode.DEVICE_LOCAL,
            fixedTimezone = null,
            missionType = MissionType.MATH,
            difficulty = Difficulty.MEDIUM,
            maxSnoozes = 3,
            snoozeMinutes = 10,
            qrExpectedCode = null
        )

        var database = AweroDatabase.createForTesting(context, name)
        val firstStore = WakeSessionStore(context, database)
        assertTrue(firstStore.start(alarm, 1234L))
        firstStore.startMission()
        firstStore.startMission()
        firstStore.markFallback()
        firstStore.markFallback()
        firstStore.setSnoozeCount(1)
        firstStore.setSnoozeCount(1)

        val sessionId = firstStore.loadActive()!!.id
        database.close()

        database = AweroDatabase.createForTesting(context, name)
        val restoredStore = WakeSessionStore(context, database)
        val restored = restoredStore.loadActive()
        assertNotNull(restored)
        assertEquals(sessionId, restored!!.id)
        assertNotNull(restored.missionStartedAt)
        assertTrue(restored.fallbackUsed)
        assertEquals(1, restored.snoozeCount)

        val events = database.wakeEvents().forSession(sessionId)
        assertEquals(listOf("TRIGGERED", "MISSION_STARTED", "FALLBACK", "SNOOZE"), events.map { it.eventType })

        val completed = restoredStore.complete()
        assertNotNull(completed)
        assertEquals("SUCCESS", completed!!.result)
        assertEquals(null, restoredStore.complete())

        database.close()
        database = AweroDatabase.createForTesting(context, name)
        assertEquals(null, database.wakeSessions().active())
        assertEquals("SUCCESS", database.wakeSessions().get(sessionId)!!.result)
        database.close()
        context.getDatabasePath(name).delete()
    }

    @Test
    fun syncQueue_duplicate_id_is_stored_once_and_survives_restart() = runBlocking {
        val name = "awero-sync-${System.nanoTime()}.db"
        var database = AweroDatabase.createForTesting(context, name)
        val id = "sync-fixed-id"
        val entity = SyncOperationEntity(
            id = id,
            operationType = "CREATE_ALARM",
            entityType = "alarm",
            entityId = "alarm-1",
            clientVersion = 1,
            payload = JSONObject().put("hour", 7).toString(),
            occurredAt = 1000L
        )
        database.syncOperations().insert(entity)
        database.syncOperations().insert(entity)
        assertEquals(1, database.syncOperations().due(Long.MAX_VALUE).size)
        database.close()

        database = AweroDatabase.createForTesting(context, name)
        assertEquals(1, database.syncOperations().due(Long.MAX_VALUE).size)
        database.close()
        context.getDatabasePath(name).delete()
    }

    @Test
    fun room_migration_1_to_2_creates_wake_events_on_real_v1_database() {
        val name = "awero-migration-${System.nanoTime()}.db"
        val file = context.getDatabasePath(name)
        if (file.exists()) file.delete()

        val legacy = Room.databaseBuilder(context, AweroDatabaseV1::class.java, name)
            .build()
        legacy.openHelper.writableDatabase
        legacy.close()

        val migrated = AweroDatabase.createForTesting(context, name)
        val tables = migrated.openHelper.writableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='wake_events'"
        )
        assertTrue(tables.moveToFirst())
        tables.close()
        migrated.close()
        file.delete()
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
abstract class AweroDatabaseV1 : RoomDatabase()
