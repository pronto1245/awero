package app.awero.core.wake

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WakeSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("awero_wake_sessions", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun save(session: WakeSession) {
        val current = load().toMutableList()
        val index = current.indexOfFirst { it.id == session.id }
        if (index >= 0) current[index] = session else current.add(session)
        prefs.edit().putString("sessions", json.encodeToString(current.takeLast(200))).apply()
    }

    fun load(): List<WakeSession> {
        val raw = prefs.getString("sessions", null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<WakeSession>>(raw) }.getOrDefault(emptyList())
    }
}
