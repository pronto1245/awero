package app.awero.core.sync

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.nio.ByteBuffer
import java.security.KeyStore
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
    fun loadSession(): AnonymousAuthSession? {
        val encoded = preferences.getString(SESSION_KEY, null) ?: return null
        return runCatching {
            val encrypted = Base64.decode(encoded, Base64.NO_WRAP)
            require(encrypted.size > GCM_IV_BYTES) { "Stored session is incomplete" }
            val buffer = ByteBuffer.wrap(encrypted)
            val iv = ByteArray(GCM_IV_BYTES).also(buffer::get)
            val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)
            val plaintext = Cipher.getInstance(CIPHER_TRANSFORMATION).run {
                init(Cipher.DECRYPT_MODE, encryptionKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
                doFinal(ciphertext)
            }
            val json = JSONObject(String(plaintext, Charsets.UTF_8))
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
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey())
        val ciphertext = cipher.doFinal(plaintext)
        val encrypted = ByteBuffer.allocate(cipher.iv.size + ciphertext.size)
            .put(cipher.iv)
            .put(ciphertext)
            .array()
        val encoded = Base64.encodeToString(encrypted, Base64.NO_WRAP)
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

    private companion object {
        const val PREFERENCES_NAME = "awero_sync_session"
        const val INSTALLATION_ID_KEY = "installation_id"
        const val SESSION_KEY = "encrypted_session"
        const val KEY_ALIAS = "awero_anonymous_auth_session"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
    }
}
