package com.nativelap.heartguard.core.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** Android Keystore의 AES-GCM 키로 SharedPreferences의 세션 토큰을 암호화한다. */
@Singleton
class AndroidKeystoreTokenStorage @Inject constructor(
    @ApplicationContext context: Context,
) : TokenStorage {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    @Synchronized
    override fun readAccessToken(): String? {
        return preferences.getString(KEY_ACCESS_TOKEN, null)?.let(::decrypt)
    }

    @Synchronized
    override fun saveAccessToken(accessToken: String) {
        require(accessToken.isNotBlank())

        val saved = preferences.edit()
            .putString(KEY_ACCESS_TOKEN, encrypt(accessToken))
            .commit()

        check(saved) { "Unable to persist the session tokens" }
    }

    @Synchronized
    override fun clear() {
        val cleared = preferences.edit()
            .remove(KEY_ACCESS_TOKEN)
            .commit()

        check(cleared) { "Unable to clear the session tokens" }
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encryptedBytes = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val encodedIv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val encodedCipherText = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        return "$encodedIv.$encodedCipherText"
    }

    private fun decrypt(value: String): String {
        val parts = value.split('.', limit = 2)
        require(parts.size == 2) { "Stored session token is malformed" }

        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val encryptedBytes = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(GCM_TAG_SIZE_BITS, iv),
        )
        return String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply {
            load(null)
        }
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE,
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return keyGenerator.generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val GCM_TAG_SIZE_BITS = 128
        const val KEY_ALIAS = "heartguard.session.aes"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val PREFERENCES_NAME = "heartguard_session"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
