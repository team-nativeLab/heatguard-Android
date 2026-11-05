package com.nativelap.heartguard.core.util

import android.content.Context
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URI
import java.security.MessageDigest
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

interface ProfilePhotoMemoryCache {
    /** 서명 query는 키에서 제외하되 URL이 갱신되면 이전 비트맵을 무효화한다. */
    fun keyFor(
        photoUrl: String?,
        sessionGeneration: Long,
        imageVariant: String,
    ): String?

    /** 사진 변경 후 해당 세션의 이전 프로필 이미지 캐시를 지운다. */
    fun invalidateSessionPhoto(sessionGeneration: Long)

    /** 사진 조회가 시작된 시점의 세션 사진 revision을 반환한다. */
    fun revisionFor(sessionGeneration: Long): Long

    /** 비동기 사진 조회 결과를 현재 revision과 대조한다. */
    fun isCurrentRevision(
        expectedRevision: Long,
        sessionGeneration: Long,
    ): Boolean

    /** 캐시 키가 세션의 현재 사진 revision에 속하는지 확인한다. */
    fun isCurrentKey(
        cacheKey: String?,
        sessionGeneration: Long,
    ): Boolean

    /** 세션 종료 후 해당 세션의 프로필 이미지 캐시와 키 상태를 지운다. */
    fun clearSession(sessionGeneration: Long)
}

internal fun hasProfilePhotoUrlChanged(
    previousFingerprint: ByteArray?,
    currentPhotoUrl: String,
): Boolean =
    previousFingerprint != null &&
        !previousFingerprint.contentEquals(currentPhotoUrl.toProfilePhotoUrlFingerprint())

private const val PROFILE_PHOTO_URL_FINGERPRINT_ALGORITHM = "SHA-256"

internal fun String.toProfilePhotoUrlFingerprint(): ByteArray =
    MessageDigest
        .getInstance(PROFILE_PHOTO_URL_FINGERPRINT_ALGORITHM)
        .digest(toByteArray(Charsets.UTF_8))

@Singleton
class CoilProfilePhotoMemoryCache
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : ProfilePhotoMemoryCache {
        private val applicationContext = context.applicationContext
        private val keyStore =
            ProfilePhotoBitmapKeyStore {
                SingletonImageLoader.get(applicationContext).memoryCache
            }

        override fun keyFor(
            photoUrl: String?,
            sessionGeneration: Long,
            imageVariant: String,
        ): String? = keyStore.keyFor(photoUrl, sessionGeneration, imageVariant)

        override fun invalidateSessionPhoto(sessionGeneration: Long) =
            keyStore.invalidateSessionPhoto(sessionGeneration)

        override fun revisionFor(sessionGeneration: Long): Long = keyStore.revisionFor(sessionGeneration)

        override fun isCurrentRevision(
            expectedRevision: Long,
            sessionGeneration: Long,
        ): Boolean = keyStore.isCurrentRevision(expectedRevision, sessionGeneration)

        override fun isCurrentKey(
            cacheKey: String?,
            sessionGeneration: Long,
        ): Boolean = keyStore.isCurrentKey(cacheKey, sessionGeneration)

        override fun clearSession(sessionGeneration: Long) = keyStore.clearSession(sessionGeneration)
    }

internal class ProfilePhotoBitmapKeyStore(
    private val memoryCacheProvider: () -> MemoryCache?,
) {
    private val cacheKeysBySession = mutableMapOf<Long, MutableMap<String, MemoryCache.Key>>()
    private val stableUrlsBySession = mutableMapOf<Long, String>()
    private val photoUrlFingerprintsBySession = mutableMapOf<Long, ByteArray>()
    private val revisionsBySession = mutableMapOf<Long, Long>()
    private val keyRevisionsBySession = mutableMapOf<Long, Long>()

    @Synchronized
    fun keyFor(
        photoUrl: String?,
        sessionGeneration: Long,
        imageVariant: String,
    ): String? {
        val currentPhotoUrl = photoUrl?.takeIf(String::isNotBlank)
        val stableUrl = currentPhotoUrl?.toStableProfilePhotoUrl()
        if (currentPhotoUrl == null || stableUrl == null) {
            removeCachedImages(sessionGeneration)
            stableUrlsBySession.remove(sessionGeneration)
            photoUrlFingerprintsBySession.remove(sessionGeneration)
            incrementKeyRevision(sessionGeneration)
            return null
        }

        val previousStableUrl = stableUrlsBySession[sessionGeneration]
        val previousFingerprint = photoUrlFingerprintsBySession[sessionGeneration]
        if (previousStableUrl != null &&
            (previousStableUrl != stableUrl || hasProfilePhotoUrlChanged(previousFingerprint, currentPhotoUrl))
        ) {
            removeCachedImages(sessionGeneration)
            incrementKeyRevision(sessionGeneration)
        }
        stableUrlsBySession[sessionGeneration] = stableUrl
        photoUrlFingerprintsBySession[sessionGeneration] = currentPhotoUrl.toProfilePhotoUrlFingerprint()

        val keyRevision = keyRevisionsBySession[sessionGeneration] ?: 0L
        val key = "profile-photo:$sessionGeneration:$keyRevision:$imageVariant:$stableUrl"
        val sessionKeys = cacheKeysBySession.getOrPut(sessionGeneration) { mutableMapOf() }
        val memoryCacheKey = MemoryCache.Key(key)
        val previousKey = sessionKeys.put(imageVariant, memoryCacheKey)
        if (previousKey != null && previousKey != memoryCacheKey) {
            memoryCacheProvider()?.remove(previousKey)
        }
        return key
    }

    @Synchronized
    fun invalidateSessionPhoto(sessionGeneration: Long) {
        removeCachedImages(sessionGeneration)
        stableUrlsBySession.remove(sessionGeneration)
        photoUrlFingerprintsBySession.remove(sessionGeneration)
        incrementRevision(sessionGeneration)
        incrementKeyRevision(sessionGeneration)
    }

    @Synchronized
    fun revisionFor(sessionGeneration: Long): Long = revisionsBySession[sessionGeneration] ?: 0L

    @Synchronized
    fun isCurrentRevision(
        expectedRevision: Long,
        sessionGeneration: Long,
    ): Boolean = revisionFor(sessionGeneration) == expectedRevision

    @Synchronized
    fun isCurrentKey(
        cacheKey: String?,
        sessionGeneration: Long,
    ): Boolean {
        val currentKeyRevision = keyRevisionsBySession[sessionGeneration] ?: 0L
        return cacheKey?.startsWith("profile-photo:$sessionGeneration:$currentKeyRevision:") == true
    }

    @Synchronized
    fun clearSession(sessionGeneration: Long) {
        removeCachedImages(sessionGeneration)
        stableUrlsBySession.remove(sessionGeneration)
        photoUrlFingerprintsBySession.remove(sessionGeneration)
        revisionsBySession.remove(sessionGeneration)
        keyRevisionsBySession.remove(sessionGeneration)
    }

    private fun incrementRevision(sessionGeneration: Long) {
        revisionsBySession[sessionGeneration] =
            (revisionsBySession[sessionGeneration] ?: 0L) + 1L
    }

    private fun incrementKeyRevision(sessionGeneration: Long) {
        keyRevisionsBySession[sessionGeneration] =
            (keyRevisionsBySession[sessionGeneration] ?: 0L) + 1L
    }

    private fun removeCachedImages(sessionGeneration: Long) {
        val sessionPrefix = "profile-photo:$sessionGeneration:"
        val trackedKeys = cacheKeysBySession.remove(sessionGeneration)?.values.orEmpty()
        val memoryCache = memoryCacheProvider() ?: return
        val cachedSessionKeys =
            memoryCache.keys.filter { memoryCacheKey -> memoryCacheKey.key.startsWith(sessionPrefix) }
        (trackedKeys + cachedSessionKeys).toSet().forEach(memoryCache::remove)
    }

    private fun String.toStableProfilePhotoUrl(): String? {
        val uri = runCatching { URI(this) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return null
        val host = uri.host?.lowercase(Locale.ROOT) ?: return null
        if (scheme != HTTPS_SCHEME || host.isBlank()) {
            return null
        }
        val port = uri.port.takeIf { it != NO_PORT }?.let { ":$it" }.orEmpty()
        return "$scheme://$host$port${uri.rawPath.orEmpty()}"
    }

    private companion object {
        const val HTTPS_SCHEME = "https"
        const val NO_PORT = -1
    }
}
