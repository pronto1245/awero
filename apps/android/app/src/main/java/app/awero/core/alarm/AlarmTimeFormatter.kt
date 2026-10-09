package app.awero.core.alarm

import java.text.DateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object AlarmTimeFormatter {
    fun format(
        hour: Int,
        minute: Int,
        timeZoneId: String? = null,
        locale: Locale = Locale.getDefault()
    ): String {
        val timeZone = timeZoneId
            ?.takeIf { it in TimeZone.getAvailableIDs() }
            ?.let(TimeZone::getTimeZone)
            ?: TimeZone.getDefault()
        val calendar = Calendar.getInstance(timeZone, locale).apply {
            clear()
            set(2024, Calendar.JANUARY, 1, hour, minute, 0)
        }
        return DateFormat.getTimeInstance(DateFormat.SHORT, locale).apply {
            this.timeZone = timeZone
        }.format(calendar.time)
    }
}
