package com.nativelap.heartguard.core.util

import coil3.ColorImage
import coil3.memory.MemoryCache
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfilePhotoMemoryCachePolicyTest {
    @Test
    fun `refreshed signed URL is considered changed even when image path is unchanged`() {
        assertTrue(
            hasProfilePhotoUrlChanged(
                previousFingerprint =
                    "https://images.example.test/avatar?signature=old"
                        .toProfilePhotoUrlFingerprint(),
                currentPhotoUrl = "https://images.example.test/avatar?signature=new",
            ),
        )
    }

    @Test
    fun `same signed URL does not invalidate bitmap`() {
        val photoUrl = "https://images.example.test/avatar?signature=current"

        assertFalse(
            hasProfilePhotoUrlChanged(
                previousFingerprint = photoUrl.toProfilePhotoUrlFingerprint(),
                currentPhotoUrl = photoUrl,
            ),
        )
    }

    @Test
    fun `first signed URL does not count as a refresh`() {
        assertFalse(
            hasProfilePhotoUrlChanged(
                previousFingerprint = null,
                currentPhotoUrl = "https://images.example.test/avatar?signature=first",
            ),
        )
    }

    @Test
    fun `refreshed signed URL removes old Coil bitmap and issues a new key`() {
        val memoryCache = MemoryCache.Builder().maxSizeBytes(1024L).build()
        val keyStore = ProfilePhotoBitmapKeyStore { memoryCache }
        val oldUrl = "https://images.example.test/avatar?signature=old"
        val newUrl = "https://images.example.test/avatar?signature=new"
        val oldCacheKey = keyStore.keyFor(oldUrl, SESSION, IMAGE_VARIANT)!!
        val oldMemoryCacheKey = MemoryCache.Key(oldCacheKey)
        memoryCache[oldMemoryCacheKey] =
            MemoryCache.Value(
                ColorImage(
                    color = 0xFF123456.toInt(),
                    width = 1,
                    height = 1,
                    size = 4L,
                ),
            )
        assertNotNull(memoryCache[oldMemoryCacheKey])

        val newCacheKey = keyStore.keyFor(newUrl, SESSION, IMAGE_VARIANT)!!

        assertNotEquals(oldCacheKey, newCacheKey)
        assertNull(memoryCache[oldMemoryCacheKey])
        assertNull(memoryCache[MemoryCache.Key(newCacheKey)])
        assertEquals(true, keyStore.isCurrentKey(newCacheKey, SESSION))
    }

    @Test
    fun `same refreshed photo result can produce keys for both screens`() {
        val keyStore = ProfilePhotoBitmapKeyStore { null }
        keyStore.keyFor(
            "https://images.example.test/avatar?signature=old",
            SESSION,
            "menu",
        )
        val expectedRequestRevision = keyStore.revisionFor(SESSION)
        val refreshedUrl = "https://images.example.test/avatar?signature=refreshed"

        val menuKey = keyStore.keyFor(refreshedUrl, SESSION, "menu")
        val profileEditKey = keyStore.keyFor(refreshedUrl, SESSION, "profile-edit")

        assertNotNull(menuKey)
        assertNotNull(profileEditKey)
        assertTrue(keyStore.isCurrentRevision(expectedRequestRevision, SESSION))
        assertTrue(keyStore.isCurrentKey(menuKey, SESSION))
        assertTrue(keyStore.isCurrentKey(profileEditKey, SESSION))
    }

    @Test
    fun `no photo response removes prior Coil bitmap and comparison fingerprint`() {
        val memoryCache = MemoryCache.Builder().maxSizeBytes(1024L).build()
        val keyStore = ProfilePhotoBitmapKeyStore { memoryCache }
        val oldCacheKey =
            keyStore.keyFor(
                "https://images.example.test/avatar?signature=old",
                SESSION,
                IMAGE_VARIANT,
            )!!
        val oldMemoryCacheKey = MemoryCache.Key(oldCacheKey)
        memoryCache[oldMemoryCacheKey] = MemoryCache.Value(ColorImage(size = 4L))

        assertNull(keyStore.keyFor(null, SESSION, IMAGE_VARIANT))

        assertNull(memoryCache[oldMemoryCacheKey])
        assertFalse(keyStore.isCurrentKey(oldCacheKey, SESSION))
        assertEquals(0L, keyStore.revisionFor(SESSION))
    }

    private companion object {
        const val SESSION = 77L
        const val IMAGE_VARIANT = "menu"
    }
}
