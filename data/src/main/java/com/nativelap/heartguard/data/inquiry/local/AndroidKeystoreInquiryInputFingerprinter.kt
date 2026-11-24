package com.nativelap.heartguard.data.inquiry.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidKeystoreInquiryInputFingerprinter
    @Inject
    constructor() : InquiryInputFingerprinter {
        override fun fingerprint(
            ownerId: String,
            title: String,
            content: String,
            createKeyIfMissing: Boolean,
        ): String {
            val canonicalInput = canonicalize(ownerId, title, content)
            val mac = Mac.getInstance(MAC_ALGORITHM)
            mac.init(getOrCreateKey(createKeyIfMissing))
            return mac.doFinal(canonicalInput).joinToString("") { byte -> "%02x".format(byte) }
        }

        private fun canonicalize(
            ownerId: String,
            title: String,
            content: String,
        ): ByteArray =
            ByteArrayOutputStream().use { output ->
                DataOutputStream(output).use { dataOutput ->
                    listOf(FINGERPRINT_DOMAIN, ownerId, title, content).forEach { field ->
                        val bytes = field.toByteArray(Charsets.UTF_8)
                        dataOutput.writeInt(bytes.size)
                        dataOutput.write(bytes)
                    }
                }
                output.toByteArray()
            }

        private fun getOrCreateKey(createKeyIfMissing: Boolean): SecretKey {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            if (existingKey != null) return existingKey
            check(createKeyIfMissing) { "Inquiry fingerprint key is missing" }

            val keyGenerator = KeyGenerator.getInstance(MAC_ALGORITHM, KEYSTORE_PROVIDER)
            keyGenerator.init(
                KeyGenParameterSpec
                    .Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
                    ).setKeySize(HMAC_KEY_SIZE_BITS)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build(),
            )
            return keyGenerator.generateKey()
        }

        private companion object {
            const val KEYSTORE_PROVIDER = "AndroidKeyStore"
            const val MAC_ALGORITHM = "HmacSHA256"
            const val KEY_ALIAS = "inquiry-submission-fingerprint-v1"
            const val HMAC_KEY_SIZE_BITS = 256
            const val FINGERPRINT_DOMAIN = "heartguard:inquiry-submission-fingerprint:v1"
        }
    }
