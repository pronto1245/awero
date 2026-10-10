package app.awero.core.alarm

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
class LegacyAlarmMigrationRecoveryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "awero-legacy-alarm-recovery.db"
    private val preferences = context.getSharedPreferences("awero_alarms", Context.MODE_PRIVATE)
    private var testDatabase: AweroDatabase? = null

    @After
    fun cleanup() {
        testDatabase?.close()
        testDatabase = null
        context.deleteDatabase(databaseName)
        preferences.edit().clear().commit()
    }

    @Test
    fun missingLegacyRecordIsNotMarkedMigratedAndCanRecoverOnRetry() = kotlinx.coroutines.runBlocking {
        val id = "alarm-recovered"
        preferences.edit().clear().putStringSet("ids", setOf(id)).commit()
        val database = AweroDatabase.createForTesting(context, databaseName).also { testDatabase = it }
        val store = AlarmStore(context, database)

        val firstAttempt = runCatching { store.all() }
        assertTrue("Missing legacy data must surface as a read failure", firstAttempt.isFailure)
        assertFalse("Failed migration must remain retryable", preferences.getBoolean("room_migrated", false))

        preferences.edit().putString("alarm:$id", JSONObject()
            .put("id", id)
            .put("version", 1)
            .put("hour", 7)
            .put("minute", 30)
            .put("enabled", true)
            .put("weekdays", JSONArray().put(1).put(2).put(3).put(4).put(5))
            .put("timezoneMode", "DEVICE_LOCAL")
            .put("missionType", "MATH")
            .put("difficulty", "MEDIUM")
            .toString()).commit()

        val recovered = store.all()
        assertEquals(listOf(id), recovered.map { it.id })
        assertTrue("Migration marker is set only after every record is persisted", preferences.getBoolean("room_migrated", false))
    }
}
