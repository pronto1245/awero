package app.awero.core.wake

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.awero.core.storage.AweroDatabase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LegacyWakeSessionMigrationRecoveryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "awero-legacy-wake-recovery.db"
    private val preferences = context.getSharedPreferences("awero_wake_sessions", Context.MODE_PRIVATE)
    private var testDatabase: AweroDatabase? = null

    @After
    fun cleanup() {
        testDatabase?.close()
        testDatabase = null
        context.deleteDatabase(databaseName)
        preferences.edit().clear().commit()
    }

    @Test
    fun invalidLegacyDataIsNotMarkedMigratedAndCanRecoverOnRetry() = kotlinx.coroutines.runBlocking {
        preferences.edit().clear().putString("sessions", "not valid JSON").commit()
        val database = AweroDatabase.createForTesting(context, databaseName).also { testDatabase = it }
        val store = WakeSessionStore(context, database)

        assertTrue("Corrupt legacy data must surface instead of looking like empty history", runCatching { store.load() }.isFailure)
        assertFalse(preferences.getBoolean("room_migrated", false))

        val legacy = JSONObject()
            .put("id", "wake-1")
            .put("alarmId", "alarm-1")
            .put("alarmVersion", 1)
            .put("scheduledAt", 1000L)
            .put("triggeredAt", 1100L)
            .put("missionStartedAt", JSONObject.NULL)
            .put("completedAt", JSONObject.NULL)
            .put("result", JSONObject.NULL)
            .put("snoozeCount", 0)
            .put("fallbackUsed", false)
            .put("emergencyStop", false)
        preferences.edit().putString("sessions", JSONArray().put(legacy).toString()).commit()

        val recovered = store.load()
        assertEquals("wake-1", recovered.single().id)
        assertTrue(preferences.getBoolean("room_migrated", false))
    }
}
