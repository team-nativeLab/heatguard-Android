package com.nativelap.heartguard.viewmodel.menu

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MenuDrawerProfileUiModelTest {
    @Test
    fun `only absolute https profile photo urls are accepted`() {
        assertTrue(isHttpsProfilePhotoUrl("https://images.example.test/profile"))
        assertTrue(isHttpsProfilePhotoUrl("HTTPS://images.example.test/profile"))
        assertFalse(isHttpsProfilePhotoUrl(null))
        assertFalse(isHttpsProfilePhotoUrl(" "))
        assertFalse(isHttpsProfilePhotoUrl("http://images.example.test/profile"))
        assertFalse(isHttpsProfilePhotoUrl("file:///profile"))
        assertFalse(isHttpsProfilePhotoUrl("https:///profile"))
        assertFalse(isHttpsProfilePhotoUrl("not a url"))
    }

    @Test
    fun `profile photo is absent by default until the server returns it`() {
        assertFalse(isHttpsProfilePhotoUrl(MenuDrawerProfileUiModel().photoUrl))
        assertTrue(
            isHttpsProfilePhotoUrl(
                MenuDrawerProfileUiModel(photoUrl = "https://images.example.test/profile").photoUrl,
            ),
        )
    }

    @Test
    fun `profile responses are rejected after the session changes`() {
        assertTrue(
            isProfileSessionCurrent(
                expectedSessionGeneration = 7L,
                ownedSessionGeneration = 7L,
                currentSessionGeneration = 7L,
            ),
        )
        assertFalse(
            isProfileSessionCurrent(
                expectedSessionGeneration = 7L,
                ownedSessionGeneration = 8L,
                currentSessionGeneration = 8L,
            ),
        )
        assertFalse(
            isProfileSessionCurrent(
                expectedSessionGeneration = 7L,
                ownedSessionGeneration = 7L,
                currentSessionGeneration = 8L,
            ),
        )
    }
}
