package com.nativelap.heartguard.core.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** Android Keystore의 AES-GCM 키로 SharedPreferences의 세션 토큰을 암호화한다. */
@Singleton
class AndroidKeystoreTokenStorage
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : TokenStorage {
        private val preferences =
            context.getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE,
            )

        /** 복호화할 수 없는 저장값(키 폐기·Keystore 손상)은 없는 토큰으로 보고 정리를 시도한다. */
        @Synchronized
        override fun readAccessToken(): String? {
            val storedValue = preferences.getString(KEY_ACCESS_TOKEN, null) ?: return null
            return try {
                decrypt(storedValue)
            } catch (_: GeneralSecurityException) {
                discardUnreadableToken()
                null
            } catch (_: IllegalArgumentException) {
                discardUnreadableToken()
                null
            }
        }

        @Synchronized
        override fun saveAccessToken(accessToken: String) {
            require(accessToken.isNotBlank())

            val saved =
                preferences
                    .edit()
                    .putString(KEY_ACCESS_TOKEN, encrypt(accessToken))
                    .commit()

            check(saved) { "Unable to persist the session tokens" }
        }

        /** 디스크 삭제가 실패하면 암호화 키를 폐기해 남은 암호문을 다시 읽을 수 없게 만든다. */
        @Synchronized
        override fun clear() {
            val cleared =
                preferences
                    .edit()
                    .remove(KEY_ACCESS_TOKEN)
                    .commit()

            if (!cleared) {
                deleteSecretKey()
            }
        }

        // 읽을 수 없는 값은 다음 시작에서도 다시 읽지 않도록 즉시 지운다. 실패해도 읽기 결과는 이미 null이다.
        private fun discardUnreadableToken() {
            preferences.edit(commit = true) {
                remove(KEY_ACCESS_TOKEN)
            }
        }

        private fun deleteSecretKey() {
            val keyStore =
                KeyStore.getInstance(ANDROID_KEY_STORE).apply {
                    load(null)
                }
            keyStore.deleteEntry(KEY_ALIAS)
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
            val keyStore =
                KeyStore.getInstance(ANDROID_KEY_STORE).apply {
                    load(null)
                }
            val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            if (existingKey != null) {
                return existingKey
            }

            val keyGenerator =
                KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEY_STORE,
                )
            keyGenerator.init(
                KeyGenParameterSpec
                    .Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
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
