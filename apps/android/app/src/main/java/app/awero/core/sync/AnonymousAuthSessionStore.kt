package app.awero.core.sync

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class AnonymousAuthSession(
    val anonymousUserId: String,
    val serverDeviceId: String,
    val accessToken: String,
    val expiresAtEpochMillis: Long
)

class AnonymousAuthSessionStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun installationId(): String {
        preferences.getString(INSTALLATION_ID_KEY, null)?.let { return it }
        val id = UUID.randomUUID().toString()
        check(preferences.edit().putString(INSTALLATION_ID_KEY, id).commit()) { "Could not save installation ID" }
        return id
    }

    @Synchronized
    fun installationSecret(): String {
        preferences.getString(INSTALLATION_SECRET_KEY, null)?.let { encoded ->
            runCatching { decrypt(encoded) }.getOrNull()?.let { secret ->
                if (secret.matches(INSTALLATION_SECRET_PATTERN)) return secret
            }
        }
        val bytes = ByteArray(32).also(SecureRandom()::nextBytes)
        val secret = Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        check(preferences.edit().putString(INSTALLATION_SECRET_KEY, encrypt(secret)).commit()) {
            "Could not save installation secret"
        }
        return secret
    }

    @Synchronized
    fun installationSecretIsBound(): Boolean = preferences.getBoolean(INSTALLATION_SECRET_BOUND_KEY, false)

    @Synchronized
    fun markInstallationSecretBound() {
        check(preferences.edit().putBoolean(INSTALLATION_SECRET_BOUND_KEY, true).commit()) {
            "Could not persist installation credential state"
        }
    }

    @Synchronized
    fun loadSession(): AnonymousAuthSession? {
        val encoded = preferences.getString(SESSION_KEY, null) ?: return null
        return runCatching {
            val json = JSONObject(decrypt(encoded))
            AnonymousAuthSession(
                anonymousUserId = json.getString("anonymousUserId"),
                serverDeviceId = json.getString("serverDeviceId"),
                accessToken = json.getString("accessToken"),
                expiresAtEpochMillis = json.getLong("expiresAtEpochMillis")
            )
        }.getOrElse {
            preferences.edit().remove(SESSION_KEY).commit()
            null
        }
    }

    @Synchronized
    fun saveSession(session: AnonymousAuthSession) {
        val plaintext = JSONObject()
            .put("anonymousUserId", session.anonymousUserId)
            .put("serverDeviceId", session.serverDeviceId)
            .put("accessToken", session.accessToken)
            .put("expiresAtEpochMillis", session.expiresAtEpochMillis)
            .toString()
            .toByteArray(Charsets.UTF_8)
        val encoded = encrypt(String(plaintext, Charsets.UTF_8))
        check(preferences.edit().putString(SESSION_KEY, encoded).commit()) { "Could not save auth session" }
    }

    @Synchronized
    fun clearSession() {
        check(preferences.edit().remove(SESSION_KEY).commit()) { "Could not clear auth session" }
    }

    private fun encryptionKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey())
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val encrypted = ByteBuffer.allocate(cipher.iv.size + ciphertext.size)
            .put(cipher.iv)
            .put(ciphertext)
            .array()
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val encrypted = Base64.decode(encoded, Base64.NO_WRAP)
        require(encrypted.size > GCM_IV_BYTES) { "Stored secret is incomplete" }
        val buffer = ByteBuffer.wrap(encrypted)
        val iv = ByteArray(GCM_IV_BYTES).also(buffer::get)
        val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)
        return Cipher.getInstance(CIPHER_TRANSFORMATION).run {
            init(Cipher.DECRYPT_MODE, encryptionKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(doFinal(ciphertext), Charsets.UTF_8)
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "awero_sync_session"
        const val INSTALLATION_ID_KEY = "installation_id"
        const val INSTALLATION_SECRET_KEY = "encrypted_installation_secret"
        const val INSTALLATION_SECRET_BOUND_KEY = "installation_secret_bound"
        val INSTALLATION_SECRET_PATTERN = Regex("^[A-Za-z0-9_-]{43}$")
        const val SESSION_KEY = "encrypted_session"
        const val KEY_ALIAS = "awero_anonymous_auth_session"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
    }
}
