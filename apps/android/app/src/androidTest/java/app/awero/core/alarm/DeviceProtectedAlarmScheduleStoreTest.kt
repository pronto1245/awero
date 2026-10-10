package app.awero.core.alarm

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Calendar
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceProtectedAlarmScheduleStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preferences = context.createDeviceProtectedStorageContext()
        .getSharedPreferences("awero_direct_boot_alarm_schedules", Context.MODE_PRIVATE)

    @Test
    fun alarmStoreKeepsRoomAndDeviceProtectedScheduleInSync() = runBlocking {
        val id = UUID.randomUUID().toString()
        val alarm = Alarm(
            id = id,
            version = 1,
            hour = 8,
            minute = 15,
            enabled = true,
            weekdays = setOf(Calendar.TUESDAY),
            missionType = MissionType.MATH,
            difficulty = Difficulty.MEDIUM
        )
        val alarmStore = AlarmStore(context)
        val scheduleStore = DeviceProtectedAlarmScheduleStore(context)
        try {
            alarmStore.save(alarm)

            assertEquals(alarm.toSchedule(), scheduleStore.get(id))
            assertEquals(alarm, alarmStore.get(id))

            alarmStore.delete(id)

            assertNull(alarmStore.get(id))
            assertNull(scheduleStore.get(id))
        } finally {
            alarmStore.delete(id)
        }
    }

    @Test
    fun persistsOnlyScheduleProjectionAndRemovesDisabledAlarms() = runBlocking {
        val id = UUID.randomUUID().toString()
        val alarm = Alarm(
            id = id,
            version = 4,
            hour = 6,
            minute = 45,
            enabled = true,
            weekdays = setOf(Calendar.MONDAY, Calendar.FRIDAY),
            timezoneMode = TimezoneMode.FIXED,
            fixedTimezone = "Europe/Moscow",
            missionType = MissionType.QR,
            difficulty = Difficulty.HARD,
            qrExpectedCode = "private-qr-value"
        )
        val store = DeviceProtectedAlarmScheduleStore(context)
        try {
            assertTrue(store.upsert(alarm.toSchedule()))

            assertEquals(alarm.toSchedule(), DeviceProtectedAlarmScheduleStore(context).get(id))
            val raw = preferences.getString("alarm:$id", null)
            assertNotNull(raw)
            assertFalse(raw.orEmpty().contains("missionType"))
            assertFalse(raw.orEmpty().contains("difficulty"))
            assertFalse(raw.orEmpty().contains("qrExpectedCode"))
            assertFalse(raw.orEmpty().contains("private-qr-value"))

            assertTrue(store.upsert(alarm.copy(enabled = false).toSchedule()))
            assertNull(store.get(id))
        } finally {
            store.remove(id)
        }
    }
}
