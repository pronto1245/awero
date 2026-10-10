package app.awero.core.sync

import android.content.Context
import app.awero.core.storage.AweroDatabase
import app.awero.core.storage.WakeSessionMapper
import org.json.JSONObject
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

class WakeSessionSyncClient(
    context: Context,
    private val configuration: SyncApiConfiguration
) {
    private val database = AweroDatabase.get(context.applicationContext)
    private val transport: SyncHttpTransport = UrlConnectionSyncHttpTransport()

    suspend fun uploadRecent(bearerToken: String, nowMillis: Long = System.currentTimeMillis()) {
        val oldestAllowed = nowMillis - MAX_AGE_MILLIS
        val latestAllowed = nowMillis + FUTURE_TOLERANCE_MILLIS
        val sessions = database.wakeSessions().recent(includeTest = false)
        for (entity in sessions) {
            val triggeredAt = entity.triggeredAt ?: continue
            if (triggeredAt !in oldestAllowed..latestAllowed) continue
            val session = WakeSessionMapper.fromEntity(entity)
            val missionType = session.missionType ?: database.alarms().get(entity.alarmId)?.missionType ?: continue
            try {
                upload(session.id, session.alarmId, session.alarmVersion, session.scheduledAt, triggeredAt,
                    missionType, session.missionStartedAt, session.completedAt, session.result,
                    session.snoozeCount, session.fallbackUsed, session.emergencyStop, bearerToken)
            } catch (_: Exception) {
                return
            }
        }
    }

    private suspend fun upload(
        sessionId: String,
        alarmId: String,
        alarmVersion: Int,
        scheduledAt: Long,
        triggeredAt: Long,
        missionType: String,
        missionStartedAt: Long?,
        completedAt: Long?,
        result: String?,
        snoozeCount: Int,
        fallbackUsed: Boolean,
        emergencyStop: Boolean,
        bearerToken: String
    ) {
        val createEndpoint = configuration.endpoint("wake-sessions") ?: return
        post(createEndpoint, JSONObject()
            .put("id", sessionId)
            .put("alarmId", alarmId)
            .put("alarmVersion", alarmVersion)
            .put("scheduledAt", Instant.ofEpochMilli(scheduledAt).toString())
            .put("triggeredAt", Instant.ofEpochMilli(triggeredAt).toString())
            .put("missionType", missionType)
            .put("eventId", sessionId), bearerToken)

        if (snoozeCount > 0) {
            append(sessionId, "SNOOZE", "snooze-$snoozeCount", triggeredAt,
                JSONObject().put("count", snoozeCount), bearerToken)
        }
        if (missionStartedAt != null) {
            append(sessionId, "MISSION_STARTED", "mission-started", missionStartedAt, JSONObject(), bearerToken)
            if (fallbackUsed) append(sessionId, "FALLBACK", "fallback", missionStartedAt, JSONObject(), bearerToken)
        }
        if (completedAt != null) {
            val eventType = when {
                emergencyStop || result == "EMERGENCY_STOP" -> "EMERGENCY_STOP"
                result == "SUCCESS" || result == "COMPLETED" -> "COMPLETED"
                result == "CANCELLED" -> "CANCELLED"
                else -> null
            }
            if (eventType != null) append(sessionId, eventType, eventType.lowercase(), completedAt, JSONObject(), bearerToken)
        }
    }

    private suspend fun append(
        sessionId: String,
        type: String,
        suffix: String,
        occurredAt: Long,
        payload: JSONObject,
        bearerToken: String
    ) {
        val endpoint = configuration.endpoint("wake-sessions/$sessionId/events") ?: return
        post(endpoint, JSONObject()
            .put("id", stableEventId(sessionId, suffix))
            .put("eventType", type)
            .put("occurredAt", Instant.ofEpochMilli(occurredAt).toString())
            .put("payload", payload), bearerToken)
    }

    private suspend fun post(endpoint: java.net.URI, body: JSONObject, bearerToken: String) {
        val response = transport.post(endpoint, bearerToken, body.toString())
        if (response.statusCode !in 200..299) {
            val message = runCatching { JSONObject(response.body).optString("message") }.getOrNull()
            throw SyncApiException(response.statusCode, message)
        }
    }

    private fun stableEventId(sessionId: String, suffix: String): String {
        val uuid = UUID.fromString(sessionId)
        val bytes = ByteArray(16)
        java.nio.ByteBuffer.wrap(bytes).putLong(uuid.mostSignificantBits).putLong(uuid.leastSignificantBits)
        val hash = MessageDigest.getInstance("SHA-256").digest(suffix.toByteArray(Charsets.UTF_8))
        System.arraycopy(hash, 0, bytes, 8, 8)
        bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
        val buffer = java.nio.ByteBuffer.wrap(bytes)
        return UUID(buffer.long, buffer.long).toString()
    }

    private companion object {
        const val MAX_AGE_MILLIS = 30L * 24 * 60 * 60 * 1000
        const val FUTURE_TOLERANCE_MILLIS = 5L * 60 * 1000
    }
}
