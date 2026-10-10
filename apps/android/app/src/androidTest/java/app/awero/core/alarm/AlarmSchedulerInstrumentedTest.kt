package app.awero.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Calendar
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmSchedulerInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val scheduler = AlarmScheduler(context)
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val scheduledAlarms = mutableListOf<Alarm>()

    @Before
    fun requireExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertTrue(
                "CI must grant SCHEDULE_EXACT_ALARM before running scheduler integration tests",
                alarmManager.canScheduleExactAlarms()
            )
        }
    }

    @After
    fun cancelTestAlarms() {
        scheduledAlarms.forEach(scheduler::cancel)
    }

    @Test
    fun schedulesOnlySelectedWeekdaysAndCancelRemovesEveryRequest() {
        val alarm = alarm(weekdays = setOf(Calendar.MONDAY, Calendar.WEDNESDAY))
        scheduledAlarms += alarm

        scheduler.schedule(alarm)

        assertTrue("Every selected weekday must be registered", scheduler.isScheduled(alarm))
        (1..7).forEach { day ->
            val exists = hasPendingIntent(alarm, day)
            if (day in alarm.weekdays) {
                assertNotNull("Expected a request for weekday $day", exists)
            } else {
                assertNull("Unexpected request for weekday $day", exists)
            }
        }

        scheduler.cancel(alarm)

        assertFalse("Cancel must clear the alarm's schedule", scheduler.isScheduled(alarm))
        (1..7).forEach { day ->
            assertNull("Cancel must clear weekday $day", hasPendingIntent(alarm, day))
        }
    }

    @Test
    fun recurringAlarmIsPublishedToTheSystemAlarmClock() {
        val alarm = alarm(weekdays = setOf(Calendar.MONDAY))
        scheduledAlarms += alarm

        scheduler.schedule(alarm)

        val nextAlarm = requireNotNull(alarmManager.nextAlarmClock) {
            "Android should expose the next AWERO alarm"
        }
        assertTrue("The exposed alarm must be in the future", nextAlarm.triggerTime > System.currentTimeMillis())
        assertEquals(context.packageName, nextAlarm.showIntent.creatorPackage)
    }

    @Test
    fun schedulingDisabledAlarmCancelsItsExistingSchedule() {
        val enabled = alarm(weekdays = setOf(Calendar.TUESDAY, Calendar.THURSDAY))
        scheduledAlarms += enabled
        scheduler.schedule(enabled)
        assertTrue(scheduler.isScheduled(enabled))

        scheduler.schedule(enabled.copy(enabled = false))

        assertFalse("Disabled alarms must not retain scheduled requests", scheduler.isScheduled(enabled))
        (1..7).forEach { day ->
            assertNull("Disabled alarm retained weekday $day", hasPendingIntent(enabled, day))
        }
    }

    private fun hasPendingIntent(alarm: Alarm, day: Int): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_ALARM
        }
        return PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode() * 31 + day,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun alarm(weekdays: Set<Int>) = Alarm(
        id = UUID.randomUUID().toString(),
        version = 1,
        hour = 7,
        minute = 30,
        enabled = true,
        weekdays = weekdays,
        missionType = MissionType.MATH,
        difficulty = Difficulty.MEDIUM
    )
}
