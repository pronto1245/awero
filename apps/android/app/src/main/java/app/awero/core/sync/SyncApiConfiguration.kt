package app.awero.core.sync

import android.content.Context
import android.content.pm.PackageManager
import java.net.URI

data class SyncApiConfiguration private constructor(val baseUrl: String?) {
    val isEnabled: Boolean get() = baseUrl != null

    fun endpoint(path: String): URI? {
        val base = baseUrl ?: return null
        val relativePath = path.trimStart('/')
        if (relativePath.isBlank() || relativePath.contains('\\') || relativePath.contains('?') || relativePath.contains('#')) {
            return null
        }
        val segments = relativePath.split('/')
        if (segments.any { segment ->
                val decoded = runCatching { URI.create("/$segment").path.removePrefix("/") }.getOrNull()
                decoded.isNullOrBlank() || decoded == "." || decoded == ".." || decoded.contains('/') || decoded.contains('\\')
            }) return null
        return runCatching { URI.create("$base/$relativePath") }.getOrNull()
    }

    companion object {
        private const val META_DATA_KEY = "app.awero.API_BASE_URL"

        fun from(context: Context): SyncApiConfiguration {
            val applicationInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            return fromValue(applicationInfo.metaData?.getString(META_DATA_KEY))
        }

        internal fun fromValue(value: String?): SyncApiConfiguration {
            val candidate = value?.trim()?.trimEnd('/')?.takeIf(String::isNotEmpty)
                ?: return SyncApiConfiguration(null)
            val uri = runCatching { URI(candidate) }.getOrNull()
            if (
                uri?.scheme?.equals("https", ignoreCase = true) != true ||
                uri.host.isNullOrBlank() || uri.userInfo != null || uri.query != null || uri.fragment != null
            ) return SyncApiConfiguration(null)
            return SyncApiConfiguration(candidate)
        }
    }
}
