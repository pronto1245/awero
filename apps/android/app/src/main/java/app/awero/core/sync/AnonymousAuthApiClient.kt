package app.awero.core.sync

import org.json.JSONObject

data class AnonymousRegistrationRequest(
    val deviceId: String,
    val installationSecret: String,
    val platform: String,
    val appVersion: String,
    val osVersion: String?,
    val timezone: String
) {
    init {
        require(deviceId.length in 16..128) { "Device ID must contain 16 to 128 characters" }
        require(installationSecret.matches(Regex("^[A-Za-z0-9_-]{43}$"))) { "Installation secret is invalid" }
        require(platform == "IOS" || platform == "ANDROID") { "Unsupported platform" }
        require(appVersion.length in 1..64) { "App version must contain 1 to 64 characters" }
        require(osVersion == null || osVersion.length <= 64) { "OS version must contain at most 64 characters" }
        require(timezone.length in 1..64) { "Timezone must contain 1 to 64 characters" }
    }
}

data class AnonymousRegistrationResponse(
    val anonymousUserId: String,
    val deviceId: String,
    val accessToken: String,
    val tokenType: String,
    val expiresInDays: Int,
    val authMode: String,
    val syncEnabled: Boolean
)

class AnonymousAuthApiClient(
    private val configuration: SyncApiConfiguration,
    private val transport: SyncHttpTransport = UrlConnectionSyncHttpTransport()
) {
    suspend fun register(request: AnonymousRegistrationRequest): AnonymousRegistrationResponse {
        val endpoint = configuration.endpoint("auth/anonymous")
            ?: throw IllegalStateException("Sync API is not configured")
        val body = JSONObject()
            .put("deviceId", request.deviceId)
            .put("installationSecret", request.installationSecret)
            .put("platform", request.platform)
            .put("appVersion", request.appVersion)
            .put("timezone", request.timezone)
        request.osVersion?.let { body.put("osVersion", it) }

        val response = transport.post(endpoint, null, body.toString())
        if (response.statusCode !in 200..299) {
            val message = runCatching { JSONObject(response.body).optString("message") }.getOrNull()
            throw SyncApiException(response.statusCode, message)
        }
        val json = try {
            JSONObject(response.body)
        } catch (error: Exception) {
            throw IllegalStateException("Anonymous auth API returned invalid JSON", error)
        }
        val result = runCatching {
            AnonymousRegistrationResponse(
                anonymousUserId = json.getString("anonymousUserId"),
                deviceId = json.getString("deviceId"),
                accessToken = json.getString("accessToken"),
                tokenType = json.getString("tokenType"),
                expiresInDays = json.getInt("expiresInDays"),
                authMode = json.getString("authMode"),
                syncEnabled = json.getBoolean("syncEnabled")
            )
        }.getOrElse { throw IllegalStateException("Anonymous auth API returned an incomplete response", it) }
        require(result.accessToken.isNotBlank() && result.tokenType.equals("Bearer", ignoreCase = true)) {
            "Anonymous auth API returned an invalid token"
        }
        return result
    }

    suspend fun bindInstallation(installationSecret: String, bearerToken: String) {
        val endpoint = configuration.endpoint("auth/anonymous/credentials")
            ?: throw IllegalStateException("Sync API is not configured")
        val response = transport.post(
            endpoint,
            bearerToken,
            JSONObject().put("installationSecret", installationSecret).toString()
        )
        if (response.statusCode !in 200..299) {
            val message = runCatching { JSONObject(response.body).optString("message") }.getOrNull()
            throw SyncApiException(response.statusCode, message)
        }
    }
}
