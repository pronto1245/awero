package app.awero.core.statistics

import android.content.Context
import app.awero.core.wake.WakeSession
import org.json.JSONObject

class StatisticsStore(context: Context) {
    private val prefs = context.getSharedPreferences("awero_statistics", Context.MODE_PRIVATE)

    fun statistics(): WakeStatistics {
        val o = JSONObject(prefs.getString("statistics", "{}") ?: "{}")
        return WakeStatistics(
            planned = o.optInt("planned", 0),
            completed = o.optInt("completed", 0),
            snoozes = o.optInt("snoozes", 0),
            fallback = o.optInt("fallback", 0),
            emergencyStops = o.optInt("emergencyStops", 0),
            totalCompletionSeconds = o.optLong("totalCompletionSeconds", 0)
        )
    }

    fun recordPlanned() {
        val s = statistics()
        save(s.copy(planned = s.planned + 1))
    }

    fun record(session: WakeSession) {
        val s = statistics()
        val completed = if (session.result == "SUCCESS") 1 else 0
        val seconds = if (session.completedAt != null) {
            ((session.completedAt!! - session.scheduledAt) / 1000).coerceAtLeast(0)
        } else 0
        save(s.copy(
            completed = s.completed + completed,
            snoozes = s.snoozes + session.snoozeCount,
            fallback = s.fallback + if (session.fallbackUsed) 1 else 0,
            emergencyStops = s.emergencyStops + if (session.emergencyStop) 1 else 0,
            totalCompletionSeconds = s.totalCompletionSeconds + seconds
        ))
    }

    private fun save(s: WakeStatistics) {
        prefs.edit().putString("statistics", JSONObject().apply {
            put("planned", s.planned)
            put("completed", s.completed)
            put("snoozes", s.snoozes)
            put("fallback", s.fallback)
            put("emergencyStops", s.emergencyStops)
            put("totalCompletionSeconds", s.totalCompletionSeconds)
        }.toString()).apply()
    }
}
