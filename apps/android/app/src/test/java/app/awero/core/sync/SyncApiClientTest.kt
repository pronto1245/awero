package app.awero.core.sync

import app.awero.core.storage.SyncOperationEntity
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncApiClientTest {
    @Test
    fun sendsIdempotentBatchAndReturnsAcceptedIdsAndConflicts() = runBlocking {
        var requestedEndpoint = ""
        var requestedToken = ""
        var requestBody = ""
        val client = SyncApiClient(
            SyncApiConfiguration.fromValue("https://api.example.test/api/v1/"),
            SyncHttpTransport { endpoint, token, body ->
                requestedEndpoint = endpoint.toString()
                requestedToken = token
                requestBody = body
                SyncHttpResponse(
                    200,
                    """
                    {"acceptedIds":["op-1"],"conflicts":[{"id":"op-2","code":"VERSION_MISMATCH","serverVersion":4,"serverEntity":{"id":"alarm-2","version":4}}],"serverTime":"2026-10-09T07:00:00Z"}
                    """.trimIndent()
                )
            }
        )
        val operations = listOf(
            operation("op-1", "alarm-1", "{\"hour\":7,\"enabled\":true}", clientVersion = 2),
            operation("op-2", "alarm-2", "{}", clientVersion = 3)
        )

        val response = client.sync("test-token", operations)

        assertEquals("https://api.example.test/api/v1/sync", requestedEndpoint)
        assertEquals("test-token", requestedToken)
        val request = JSONObject(requestBody).getJSONArray("operations")
        assertEquals(2, request.length())
        assertEquals("op-1", request.getJSONObject(0).getString("id"))
        assertEquals(7, request.getJSONObject(0).getJSONObject("payload").getInt("hour"))
        assertTrue(request.getJSONObject(0).getString("occurredAt").endsWith("Z"))
        assertEquals(setOf("op-1"), response.acceptedIds)
        assertEquals("VERSION_MISMATCH", response.conflicts.single().code)
        assertEquals(4, response.conflicts.single().serverVersion)
        assertTrue(response.conflicts.single().serverEntityJson!!.contains("alarm-2"))
        assertEquals("2026-10-09T07:00:00Z", response.serverTime)
    }

    @Test
    fun httpErrorsRetainStatusAndDoNotLookLikeAcceptedOperations() = runBlocking {
        val client = SyncApiClient(
            SyncApiConfiguration.fromValue("https://api.example.test/api/v1"),
            SyncHttpTransport { _, _, _ -> SyncHttpResponse(401, "{\"message\":\"INVALID_TOKEN\"}") }
        )

        val error = assertThrows(SyncApiException::class.java) {
            runBlocking { client.sync("expired-token", listOf(operation("op-1", "alarm-1", "{}"))) }
        }

        assertEquals(401, error.statusCode)
        assertEquals("INVALID_TOKEN", error.message)
    }

    @Test
    fun missingConfigurationNeverCallsTransport() = runBlocking {
        var sent = false
        val client = SyncApiClient(
            SyncApiConfiguration.fromValue(null),
            SyncHttpTransport { _, _, _ ->
                sent = true
                SyncHttpResponse(200, "{}")
            }
        )

        assertThrows(IllegalStateException::class.java) {
            runBlocking { client.sync("token", listOf(operation("op-1", "alarm-1", "{}"))) }
        }
        assertFalse(sent)
    }

    private fun operation(
        id: String,
        entityId: String,
        payload: String,
        clientVersion: Int? = null
    ) = SyncOperationEntity(
        id = id,
        operationType = "UPDATE_ALARM",
        entityType = "ALARM",
        entityId = entityId,
        clientVersion = clientVersion,
        payload = payload,
        occurredAt = 1_791_523_200_000L
    )
}
