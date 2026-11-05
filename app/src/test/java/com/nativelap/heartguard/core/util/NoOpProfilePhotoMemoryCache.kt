package com.nativelap.heartguard.core.util

internal object NoOpProfilePhotoMemoryCache : ProfilePhotoMemoryCache {
    override fun keyFor(
        photoUrl: String?,
        sessionGeneration: Long,
        imageVariant: String,
    ): String? = null

    override fun invalidateSessionPhoto(sessionGeneration: Long) = Unit

    override fun revisionFor(sessionGeneration: Long): Long = 0L

    override fun isCurrentRevision(
        expectedRevision: Long,
        sessionGeneration: Long,
    ): Boolean = expectedRevision == 0L

    override fun isCurrentKey(
        cacheKey: String?,
        sessionGeneration: Long,
    ): Boolean = false

    override fun clearSession(sessionGeneration: Long) = Unit
}

internal class RevisionedProfilePhotoMemoryCache : ProfilePhotoMemoryCache {
    private val revisionsBySession = mutableMapOf<Long, Long>()
    private val keyRevisionsBySession = mutableMapOf<Long, Long>()

    override fun keyFor(
        photoUrl: String?,
        sessionGeneration: Long,
        imageVariant: String,
    ): String? =
        photoUrl?.let {
            "profile-photo:$sessionGeneration:${keyRevisionFor(sessionGeneration)}:$imageVariant:$it"
        }

    override fun invalidateSessionPhoto(sessionGeneration: Long) {
        revisionsBySession[sessionGeneration] = revisionFor(sessionGeneration) + 1L
        keyRevisionsBySession[sessionGeneration] = keyRevisionFor(sessionGeneration) + 1L
    }

    override fun revisionFor(sessionGeneration: Long): Long =
        revisionsBySession[sessionGeneration] ?: 0L

    override fun isCurrentRevision(
        expectedRevision: Long,
        sessionGeneration: Long,
    ): Boolean = revisionFor(sessionGeneration) == expectedRevision

    override fun isCurrentKey(
        cacheKey: String?,
        sessionGeneration: Long,
    ): Boolean = cacheKey?.contains(":${keyRevisionFor(sessionGeneration)}:") == true

    override fun clearSession(sessionGeneration: Long) {
        revisionsBySession.remove(sessionGeneration)
        keyRevisionsBySession.remove(sessionGeneration)
    }

    private fun keyRevisionFor(sessionGeneration: Long): Long =
        keyRevisionsBySession[sessionGeneration] ?: 0L
}
