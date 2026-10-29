package com.nativelap.heartguard.data.record

import com.nativelap.heartguard.data.profile.dto.ProfilePhotoUploadResponseDto
import com.nativelap.heartguard.data.profile.dto.hasMatchingContentLength
import com.nativelap.heartguard.data.record.dto.UploadFileRequestDto
import com.nativelap.heartguard.data.record.dto.UploadRequestDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FieldPhotoContractTest {
    @Test
    fun `upload request contains only content type and actual size`() {
        val request =
            UploadRequestDto(
                files =
                    listOf(
                        UploadFileRequestDto(
                            contentType = "image/jpeg",
                            size = 1,
                        ),
                    ),
            )

        assertEquals(
            """{"files":[{"contentType":"image/jpeg","size":1}]}""",
            Json.encodeToString(request),
        )
    }

    @Test
    fun `photo size accepts one byte through five mebibytes`() {
        assertTrue(FieldPhotoValidation.isSupportedSize(1))
        assertTrue(FieldPhotoValidation.isSupportedSize(5L * 1024L * 1024L))
        assertFalse(FieldPhotoValidation.isSupportedSize(0))
        assertFalse(FieldPhotoValidation.isSupportedSize(5L * 1024L * 1024L + 1))
    }

    @Test
    fun `image signature identifies jpeg png and webp`() {
        assertEquals(
            "image/jpeg",
            FieldPhotoValidation.detectMimeType(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())),
        )
        assertEquals(
            "image/png",
            FieldPhotoValidation.detectMimeType(
                byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A),
            ),
        )
        assertEquals(
            "image/webp",
            FieldPhotoValidation.detectMimeType(
                byteArrayOf(0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50),
            ),
        )
        assertNull(FieldPhotoValidation.detectMimeType("not an image".encodeToByteArray()))
    }

    @Test
    fun `record upload allows one or two photos and preserves no photo records`() {
        assertTrue(FieldPhotoValidation.isAllowedUploadCount(1))
        assertTrue(FieldPhotoValidation.isAllowedUploadCount(2))
        assertFalse(FieldPhotoValidation.isAllowedUploadCount(0))
        assertFalse(FieldPhotoValidation.isAllowedUploadCount(3))
    }

    @Test
    fun `presigned url must be https and unexpired`() {
        val now = Instant.parse("2026-10-29T03:00:00Z")
        val validExpiry = "2026-10-29T03:05:00Z"

        assertTrue(
            FieldPhotoValidation.isUploadUsableAt("https://s3.example.test/upload?signature=abc", validExpiry, now),
        )
        assertFalse(FieldPhotoValidation.isUploadUsableAt("http://s3.example.test/upload", validExpiry, now))
        assertFalse(FieldPhotoValidation.isUploadUsableAt("/relative/upload", validExpiry, now))
        assertFalse(FieldPhotoValidation.isUploadUsableAt("https://[invalid", validExpiry, now))
        assertFalse(FieldPhotoValidation.isUploadUsableAt("https://s3.example.test/upload", "invalid", now))
        assertFalse(
            FieldPhotoValidation.isUploadUsableAt(
                "https://s3.example.test/upload",
                "2026-10-29T02:59:59Z",
                now,
            ),
        )
    }

    @Test
    fun `profile upload requires content length matching actual payload size`() {
        val validUpload =
            ProfilePhotoUploadResponseDto(
                uploadId = "upload-1",
                uploadUrl = "https://s3.example.test/upload",
                expiresAt = "2026-10-29T03:05:00Z",
                requiredHeaders = mapOf("Content-Length" to "123456"),
            )
        val lowerCaseHeaderUpload =
            validUpload.copy(
                requiredHeaders = mapOf("content-length" to "123456"),
            )
        val missingContentLengthUpload = validUpload.copy(requiredHeaders = emptyMap())
        val mismatchedContentLengthUpload =
            validUpload.copy(
                requiredHeaders = mapOf("Content-Length" to "123455"),
            )
        val conflictingContentLengthUpload =
            validUpload.copy(
                requiredHeaders =
                    mapOf(
                        "Content-Length" to "123456",
                        "content-length" to "123456",
                    ),
            )

        assertTrue(validUpload.hasMatchingContentLength(123456))
        assertTrue(lowerCaseHeaderUpload.hasMatchingContentLength(123456))
        assertFalse(missingContentLengthUpload.hasMatchingContentLength(123456))
        assertFalse(mismatchedContentLengthUpload.hasMatchingContentLength(123456))
        assertFalse(conflictingContentLengthUpload.hasMatchingContentLength(123456))
    }
}
