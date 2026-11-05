package com.nativelap.heartguard.view.component.profile

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileAvatarHeaderCachePolicyTest {
    @Test
    fun `local photo preview does not enter Coil memory cache`() {
        assertFalse(shouldCacheProfilePhotoInMemory(hasLocalPhotoUri = true))
    }

    @Test
    fun `server profile photo may use memory cache`() {
        assertTrue(shouldCacheProfilePhotoInMemory(hasLocalPhotoUri = false))
    }
}
