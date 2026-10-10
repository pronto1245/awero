package app.awero.core.sync

import app.awero.core.storage.SyncOperationEntity
import app.awero.core.storage.AnalyticsEventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.time.Instant

data class SyncHttpResponse(val statusCode: Int, val body: String)

fun interface SyncHttpTransport {
    suspend fun post(endpoint: URI, bearerToken: String?, body: String): SyncHttpResponse
    suspend fun get(endpoint: URI, bearerToken: String): SyncHttpResponse =
        throw UnsupportedOperationException("HTTP GET transport is unavailable")
}

data class SyncConflict(
    val id: String,
    val code: String,
    val serverVersion: Int?,
    val serverEntityJson: String?
)

data class SyncRejection(val id: String, val code: String)

data class SyncBatchResponse(
    val acceptedIds: Set<String>,
    val conflicts: List<SyncConflict>,
    val rejected: List<SyncRejection>,
    val serverTime: String?
)

data class AnalyticsBatchResponse(val acceptedIds: Set<String>, val rejected: List<SyncRejection>)
data class ServerAlarmSnapshot(val alarm: app.awero.core.alarm.Alarm, val status: String)

class SyncApiException(val statusCode: Int, message: String?) : Exception(message ?: "Sync API request failed")

class SyncApiClient(
    private val configuration: SyncApiConfiguration,
    private val transport: SyncHttpTransport = UrlConnectionSyncHttpTransport()
) {
    suspend fun sync(bearerToken: String, operations: List<SyncOperationEntity>): SyncBatchResponse {
        require(bearerToken.isNotBlank()) { "A bearer token is required" }
        require(operations.size <= 100) { "Sync batches are limited to 100 operations" }
        val endpoint = configuration.endpoint("sync") ?: throw IllegalStateException("Sync API is not configured")
        val response = transport.post(endpoint, bearerToken, encodeOperations(operations))
        if (response.statusCode !in 200..299) {
            val message = runCatching { JSONObject(response.body).optString("message") }.getOrNull()
            throw SyncApiException(response.statusCode, message)
        }
        return decodeResponse(response.body)
    }

    suspend fun sendAnalytics(bearerToken: String, events: List<AnalyticsEventEntity>): AnalyticsBatchResponse {
        require(bearerToken.isNotBlank()) { "A bearer token is required" }
        require(events.size <= 100) { "Analytics batches are limited to 100 events" }
        val endpoint = configuration.endpoint("analytics/events") ?: throw IllegalStateException("Sync API is not configured")
        val encodedEvents = JSONArray()
        events.forEach { event ->
            val properties = runCatching { JSONObject(event.payload) }
                .getOrElse { throw IllegalArgumentException("Analytics event ${event.id} has invalid JSON payload", it) }
            encodedEvents.put(JSONObject()
                .put("id", event.id)
                .put("eventName", event.eventName)
                .put("eventVersion", event.eventVersion)
                .put("properties", properties)
                .put("occurredAt", Instant.ofEpochMilli(event.occurredAt).toString()))
        }
        val response = transport.post(endpoint, bearerToken, JSONObject().put("events", encodedEvents).toString())
        if (response.statusCode !in 200..299) {
            val message = runCatching { JSONObject(response.body).optString("message") }.getOrNull()
            throw SyncApiException(response.statusCode, message)
        }
        val json = runCatching { JSONObject(response.body) }
            .getOrElse { throw IllegalStateException("Analytics API returned invalid JSON", it) }
        val accepted = json.optJSONArray("acceptedIds")
            ?: throw IllegalStateException("Analytics API response is missing acceptedIds")
        val acceptedIds = buildSet {
            for (index in 0 until accepted.length()) accepted.optString(index).takeIf(String::isNotBlank)?.let(::add)
        }
        val rejectedArray = json.optJSONArray("rejected")
        val rejected = buildList {
            for (index in 0 until (rejectedArray?.length() ?: 0)) {
                val item = rejectedArray?.optJSONObject(index) ?: continue
                val id = item.optString("id").takeIf(String::isNotBlank) ?: continue
                val code = item.optString("code").takeIf(String::isNotBlank) ?: continue
                add(SyncRejection(id, code))
            }
        }
        return AnalyticsBatchResponse(acceptedIds, rejected)
    }

    suspend fun fetchServerAlarms(bearerToken: String): List<ServerAlarmSnapshot> {
        require(bearerToken.isNotBlank()) { "A bearer token is required" }
        val endpoint = configuration.endpoint("sync/alarms") ?: throw IllegalStateException("Sync API is not configured")
        val response = transport.get(endpoint, bearerToken)
        if (response.statusCode !in 200..299) {
            val message = runCatching { JSONObject(response.body).optString("message") }.getOrNull()
            throw SyncApiException(response.statusCode, message)
        }
        val items = runCatching { JSONObject(response.body).getJSONArray("items") }
            .getOrElse { throw IllegalStateException("Alarm API response is missing items", it) }
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val weekdaysJson = item.optJSONArray("weekdays") ?: continue
                val weekdays = buildSet {
                    for (dayIndex in 0 until weekdaysJson.length()) add(weekdaysJson.optInt(dayIndex))
                }
                val alarm = runCatching {
                    app.awero.core.alarm.Alarm(
                        id = item.getString("id"),
                        version = item.getInt("version"),
                        hour = item.getInt("hour"),
                        minute = item.getInt("minute"),
                        enabled = item.getBoolean("enabled"),
                        weekdays = weekdays,
                        timezoneMode = app.awero.core.alarm.TimezoneMode.valueOf(item.getString("timezoneMode")),
                        fixedTimezone = item.optString("fixedTimezone").takeIf(String::isNotBlank),
                        missionType = app.awero.core.alarm.MissionType.valueOf(item.getString("missionType")),
                        difficulty = app.awero.core.alarm.Difficulty.valueOf(item.getString("difficulty")),
                        maxSnoozes = item.getInt("maxSnoozes"),
                        snoozeMinutes = item.getInt("snoozeMinutes"),
                        qrExpectedCode = item.optString("qrExpectedCode").takeIf(String::isNotBlank),
                        label = item.optString("label", "Alarm"),
                        snoozeEnabled = item.optBoolean("snoozeEnabled", true)
                    )
                }.getOrElse { throw IllegalStateException("Alarm API returned an invalid alarm", it) }
                add(ServerAlarmSnapshot(alarm, item.optString("status")))
            }
        }
    }

    private fun encodeOperations(operations: List<SyncOperationEntity>): String {
        val batch = JSONObject()
        val encodedOperations = JSONArray()
        operations.forEach { operation ->
            val payload = runCatching { JSONObject(operation.payload) }
                .getOrElse { throw IllegalArgumentException("Sync operation ${operation.id} has invalid JSON payload", it) }
            val encoded = JSONObject()
                .put("id", operation.id)
                .put("operationType", operation.operationType)
                .put("entityType", operation.entityType)
                .put("entityId", operation.entityId)
                .put("payload", payload)
                .put("occurredAt", Instant.ofEpochMilli(operation.occurredAt).toString())
            operation.clientVersion?.let { encoded.put("clientVersion", it) }
            encodedOperations.put(encoded)
        }
        batch.put("operations", encodedOperations)
        return batch.toString()
    }

    private fun decodeResponse(body: String): SyncBatchResponse {
        val response = try {
            JSONObject(body)
        } catch (error: Exception) {
            throw IllegalStateException("Sync API returned invalid JSON", error)
        }
        val accepted = response.optJSONArray("acceptedIds")
            ?: throw IllegalStateException("Sync API response is missing acceptedIds")
        val acceptedIds = buildSet {
            for (index in 0 until accepted.length()) {
                accepted.optString(index).takeIf(String::isNotBlank)?.let(::add)
            }
        }
        val conflictArray = response.optJSONArray("conflicts")
            ?: throw IllegalStateException("Sync API response is missing conflicts")
        val conflicts = buildList {
            for (index in 0 until conflictArray.length()) {
                val conflict = conflictArray.optJSONObject(index) ?: continue
                val id = conflict.optString("id").takeIf(String::isNotBlank) ?: continue
                val code = conflict.optString("code").takeIf(String::isNotBlank) ?: continue
                val serverVersion = if (conflict.isNull("serverVersion")) null else conflict.optInt("serverVersion")
                val serverEntity = conflict.opt("serverEntity")
                    ?.takeUnless { it == JSONObject.NULL }
                    ?.let { if (it is JSONObject) it.toString() else null }
                add(SyncConflict(id, code, serverVersion, serverEntity))
            }
        }
        val rejectedArray = response.optJSONArray("rejected")
        val rejected = buildList {
            for (index in 0 until (rejectedArray?.length() ?: 0)) {
                val item = rejectedArray?.optJSONObject(index) ?: continue
                val id = item.optString("id").takeIf(String::isNotBlank) ?: continue
                val code = item.optString("code").takeIf(String::isNotBlank) ?: continue
                add(SyncRejection(id, code))
            }
        }
        return SyncBatchResponse(
            acceptedIds = acceptedIds,
            conflicts = conflicts,
            rejected = rejected,
            serverTime = response.optString("serverTime").takeIf(String::isNotBlank)
        )
    }
}

internal class UrlConnectionSyncHttpTransport : SyncHttpTransport {
    override suspend fun post(endpoint: URI, bearerToken: String?, body: String): SyncHttpResponse =
        withContext(Dispatchers.IO) {
            require(endpoint.scheme.equals("https", ignoreCase = true)) { "Sync requests require HTTPS" }
            val connection = endpoint.toURL().openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 10_000
                connection.readTimeout = 15_000
                connection.instanceFollowRedirects = false
                connection.doOutput = true
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                bearerToken?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                SyncHttpResponse(status, responseBody)
            } finally {
                connection.disconnect()
            }
        }

    override suspend fun get(endpoint: URI, bearerToken: String): SyncHttpResponse =
        withContext(Dispatchers.IO) {
            require(endpoint.scheme.equals("https", ignoreCase = true)) { "Sync requests require HTTPS" }
            val connection = endpoint.toURL().openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10_000
                connection.readTimeout = 15_000
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $bearerToken")
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                SyncHttpResponse(status, responseBody)
            } finally {
                connection.disconnect()
            }
        }
}
