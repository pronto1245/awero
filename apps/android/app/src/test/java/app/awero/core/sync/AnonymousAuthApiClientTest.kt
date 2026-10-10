package app.awero.core.sync

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnonymousAuthApiClientTest {
    @Test
    fun registersWithoutBearerAndDecodesIssuedToken() = runBlocking {
        var endpoint = ""
        var bearerToken: String? = "unexpected"
        var requestBody = ""
        val client = AnonymousAuthApiClient(
            SyncApiConfiguration.fromValue("https://api.example.test/api/v1/"),
            SyncHttpTransport { url, token, body ->
                endpoint = url.toString()
                bearerToken = token
                requestBody = body
                SyncHttpResponse(
                    201,
                    """{"anonymousUserId":"user-1","deviceId":"device-1","accessToken":"secret","tokenType":"Bearer","expiresInDays":30,"authMode":"anonymous","syncEnabled":true}"""
                )
            }
        )

        val result = client.register(
            AnonymousRegistrationRequest("device-installation-1234", "a".repeat(43), "ANDROID", "0.1.0", "16", "Europe/Moscow")
        )

        assertEquals("https://api.example.test/api/v1/auth/anonymous", endpoint)
        assertNull(bearerToken)
        val body = JSONObject(requestBody)
        assertEquals("device-installation-1234", body.getString("deviceId"))
        assertEquals("a".repeat(43), body.getString("installationSecret"))
        assertEquals("ANDROID", body.getString("platform"))
        assertEquals("16", body.getString("osVersion"))
        assertEquals("Europe/Moscow", body.getString("timezone"))
        assertEquals("secret", result.accessToken)
        assertEquals("user-1", result.anonymousUserId)
        assertEquals(30, result.expiresInDays)
        assertEquals(true, result.syncEnabled)
    }

    @Test
    fun registrationRejectsInvalidDeviceIdAndRetainsHttpErrors() = runBlocking {
        assertThrows(IllegalArgumentException::class.java) {
            AnonymousRegistrationRequest("short", "b".repeat(43), "ANDROID", "0.1.0", null, "UTC")
        }
        val client = AnonymousAuthApiClient(
            SyncApiConfiguration.fromValue("https://api.example.test/api/v1"),
            SyncHttpTransport { _, token, _ ->
                assertNull(token)
                SyncHttpResponse(400, "{\"message\":\"INVALID_REQUEST\"}")
            }
        )

        val error = assertThrows(SyncApiException::class.java) {
            runBlocking {
                client.register(AnonymousRegistrationRequest("device-installation-1234", "c".repeat(43), "ANDROID", "0.1.0", null, "UTC"))
            }
        }
        assertEquals(400, error.statusCode)
        assertEquals("INVALID_REQUEST", error.message)
    }

    @Test
    fun missingConfigurationNeverCallsTransport() = runBlocking {
        var sent = false
        val client = AnonymousAuthApiClient(
            SyncApiConfiguration.fromValue(null),
            SyncHttpTransport { _, _, _ ->
                sent = true
                SyncHttpResponse(201, "{}")
            }
        )
        val registration = AnonymousRegistrationRequest("device-installation-1234", "d".repeat(43), "ANDROID", "0.1.0", null, "UTC")

        assertThrows(IllegalStateException::class.java) {
            runBlocking { client.register(registration) }
        }
        assertEquals(false, sent)
    }
}
