package com.nativelap.heartguard.data.profile.mapper

import com.nativelap.heartguard.data.profile.dto.ProfilePhotoResponseDto
import com.nativelap.heartguard.data.profile.dto.WorkerProfileResponseDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfileMapperTest {
    @Test
    fun `profile photo mapper preserves nullable url and expiry`() {
        val photo = ProfilePhotoResponseDto(photoUrl = null, expiresAt = null).toDomain()

        assertNull(photo.photoUrl)
        assertNull(photo.expiresAt)
    }

    @Test
    fun `profile photo mapper preserves url and expiry`() {
        val photo =
            ProfilePhotoResponseDto(
                photoUrl = "https://images.example.test/profile.jpg",
                expiresAt = "2026-10-28T12:00:00+09:00",
            ).toDomain()

        assertEquals("https://images.example.test/profile.jpg", photo.photoUrl)
        assertEquals("2026-10-28T12:00:00+09:00", photo.expiresAt)
    }

    @Test
    fun `worker profile mapper normalizes blank optional fields`() {
        val profile =
            WorkerProfileResponseDto(
                userId = "user-1",
                name = "  ",
                email = "",
                companyName = " 현장 회사 ",
                version = 7,
            ).toDomain()

        assertEquals("user-1", profile.userId)
        assertNull(profile.name)
        assertNull(profile.email)
        assertEquals(" 현장 회사 ", profile.companyName)
        assertEquals(7L, profile.version)
    }
}
