package app.awero.core.statistics

import android.content.Context
import app.awero.core.storage.AweroDatabase
import app.awero.core.storage.StatisticsEntity
import app.awero.core.wake.WakeSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

class StatisticsStore(context: Context) {
    private val appContext = context.applicationContext
    private val database = AweroDatabase.get(appContext)
    private val preferences = appContext.getSharedPreferences("awero_statistics", Context.MODE_PRIVATE)
    private val mutex = Mutex()

    @Volatile
    private var cached = legacyStatistics()

    fun statistics(): WakeStatistics = cached

    suspend fun recordPlanned() {
        mutex.withLock {
            migrateLegacyIfNeeded()
            val next = database.statistics().get()?.toModel() ?: cached
            cached = next.copy(planned = next.planned + 1)
            database.statistics().upsert(cached.toEntity())
        }
    }

    suspend fun record(session: WakeSession) {
        mutex.withLock {
            migrateLegacyIfNeeded()
            val current = database.statistics().get()?.toModel() ?: cached
            val completed = if (session.result == "SUCCESS") 1 else 0
            val seconds = if (session.completedAt != null) {
                ((session.completedAt!! - (session.triggeredAt ?: session.scheduledAt)) / 1000).coerceAtLeast(0)
            } else 0
            cached = current.copy(
                completed = current.completed + completed,
                snoozes = current.snoozes + session.snoozeCount,
                fallback = current.fallback + if (session.fallbackUsed) 1 else 0,
                emergencyStops = current.emergencyStops + if (session.emergencyStop) 1 else 0,
                totalCompletionSeconds = current.totalCompletionSeconds + seconds
            )
            database.statistics().upsert(cached.toEntity())
        }
    }

    private suspend fun migrateLegacyIfNeeded() {
        if (preferences.getBoolean("room_migrated", false)) return
        database.statistics().upsert(cached.toEntity())
        preferences.edit().putBoolean("room_migrated", true).apply()
    }

    private fun legacyStatistics(): WakeStatistics {
        val o = JSONObject(preferences.getString("statistics", "{}") ?: "{}")
        return WakeStatistics(
            planned = o.optInt("planned", 0),
            completed = o.optInt("completed", 0),
            snoozes = o.optInt("snoozes", 0),
            fallback = o.optInt("fallback", 0),
            emergencyStops = o.optInt("emergencyStops", 0),
            totalCompletionSeconds = o.optLong("totalCompletionSeconds", 0)
        )
    }

    private fun WakeStatistics.toEntity() = StatisticsEntity(
        planned = planned,
        completed = completed,
        snoozes = snoozes,
        fallback = fallback,
        emergencyStops = emergencyStops,
        totalCompletionSeconds = totalCompletionSeconds
    )

    private fun StatisticsEntity.toModel() = WakeStatistics(
        planned = planned,
        completed = completed,
        snoozes = snoozes,
        fallback = fallback,
        emergencyStops = emergencyStops,
        totalCompletionSeconds = totalCompletionSeconds
    )
}
