package app.awero.core.sync

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TimeZone

class AnonymousAuthCoordinator(private val context: Context) {
    suspend fun refreshIfNeeded(nowMillis: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        runCatching {
            val configuration = SyncApiConfiguration.from(context)
            if (!configuration.isEnabled) return@runCatching

            val store = AnonymousAuthSessionStore(context)
            val existing = store.loadSession()
            if (existing != null && existing.expiresAtEpochMillis - nowMillis > REFRESH_BEFORE_EXPIRY_MILLIS) {
                return@runCatching
            }

            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val registration = AnonymousRegistrationRequest(
                deviceId = store.installationId(),
                platform = "ANDROID",
                appVersion = packageInfo.versionName?.takeIf(String::isNotBlank) ?: "0.1.0",
                osVersion = Build.VERSION.RELEASE?.take(64),
                timezone = TimeZone.getDefault().id
            )
            val response = AnonymousAuthApiClient(configuration).register(registration)
            store.saveSession(
                AnonymousAuthSession(
                    anonymousUserId = response.anonymousUserId,
                    serverDeviceId = response.deviceId,
                    accessToken = response.accessToken,
                    expiresAtEpochMillis = nowMillis + response.expiresInDays * MILLIS_PER_DAY
                )
            )
        }
    }

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
        const val REFRESH_BEFORE_EXPIRY_MILLIS = 5L * MILLIS_PER_DAY
    }
}
