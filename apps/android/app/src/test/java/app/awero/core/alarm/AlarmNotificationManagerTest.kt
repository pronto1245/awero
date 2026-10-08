package app.awero.core.alarm

import android.app.Notification
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AlarmNotificationManagerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun alarmNotificationUsesFullScreenAlarmIntentAndStaysOngoing() {
        val notification = AlarmNotificationManager.build(
            context = context,
            alarmId = "test-alarm",
            version = 1,
            scheduledAt = 1_800_000_000_000L,
            test = false
        )

        assertEquals(Notification.CATEGORY_ALARM, notification.category)
        assertNotNull(notification.fullScreenIntent)
        assertNotNull(notification.contentIntent)
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
    }
}
