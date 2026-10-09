package app.awero.core.sync

import app.awero.core.storage.SyncOperationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.time.Instant

data class SyncHttpResponse(val statusCode: Int, val body: String)

fun interface SyncHttpTransport {
    suspend fun post(endpoint: URI, bearerToken: String, body: String): SyncHttpResponse
}

data class SyncConflict(
    val id: String,
    val code: String,
    val serverVersion: Int?,
    val serverEntityJson: String?
)

data class SyncBatchResponse(
    val acceptedIds: Set<String>,
    val conflicts: List<SyncConflict>,
    val serverTime: String?
)

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
        return SyncBatchResponse(
            acceptedIds = acceptedIds,
            conflicts = conflicts,
            serverTime = response.optString("serverTime").takeIf(String::isNotBlank)
        )
    }
}

private class UrlConnectionSyncHttpTransport : SyncHttpTransport {
    override suspend fun post(endpoint: URI, bearerToken: String, body: String): SyncHttpResponse =
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
                connection.setRequestProperty("Authorization", "Bearer $bearerToken")
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                SyncHttpResponse(status, responseBody)
            } finally {
                connection.disconnect()
            }
        }
}
