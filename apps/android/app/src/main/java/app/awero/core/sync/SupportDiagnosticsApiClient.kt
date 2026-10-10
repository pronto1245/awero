package app.awero.core.sync

import android.content.Context
import android.os.Build
import org.json.JSONObject
import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class SupportDiagnosticsApiClient(
    context: Context,
    private val configuration: SyncApiConfiguration,
    private val transport: SyncHttpTransport = UrlConnectionSyncHttpTransport()
) {
    private val appContext = context.applicationContext

    suspend fun reportSyncServerFailure(statusCode: Int, bearerToken: String, installationId: String) {
        val endpoint = configuration.endpoint("support/diagnostics") ?: return
        val day = LocalDate.now(ZoneOffset.UTC).toString()
        val id = stableId(installationId, "sync-server-failure-$day")
        val version = runCatching {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName
        }.getOrNull()?.takeIf(String::isNotBlank) ?: "unknown"
        val diagnostics = JSONObject()
            .put("platform", "ANDROID")
            .put("appVersion", version.take(40))
            .put("osVersion", Build.VERSION.RELEASE?.take(64) ?: "unknown")
            .put("timezone", java.util.TimeZone.getDefault().id.take(64))
            .put("errorCode", "SYNC_HTTP_$statusCode")
        runCatching {
            transport.post(endpoint, bearerToken,
                JSONObject().put("id", id).put("category", "SYNC").put("diagnostics", diagnostics).toString())
        }
    }

    private fun stableId(installationId: String, suffix: String): String {
        val namespace = UUID.fromString(installationId)
        val bytes = ByteArray(16)
        java.nio.ByteBuffer.wrap(bytes).putLong(namespace.mostSignificantBits).putLong(namespace.leastSignificantBits)
        val hash = MessageDigest.getInstance("SHA-256").digest(suffix.toByteArray(Charsets.UTF_8))
        System.arraycopy(hash, 0, bytes, 8, 8)
        bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
        val buffer = java.nio.ByteBuffer.wrap(bytes)
        return UUID(buffer.long, buffer.long).toString()
    }
}
