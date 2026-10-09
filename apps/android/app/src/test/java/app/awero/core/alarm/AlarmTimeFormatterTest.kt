package app.awero.core.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class AlarmTimeFormatterTest {
    @Test
    fun usLocaleUsesLocalizedTwelveHourTime() {
        val time = AlarmTimeFormatter.format(7, 0, "UTC", Locale.US)

        assertTrue(time.contains("7"))
        assertTrue(time.contains("AM", ignoreCase = true))
        assertTrue(time != "07:00")
    }

    @Test
    fun russianLocaleUsesLocalizedTwentyFourHourTime() {
        assertEquals("07:00", AlarmTimeFormatter.format(7, 0, "UTC", Locale("ru", "RU")))
    }
}
