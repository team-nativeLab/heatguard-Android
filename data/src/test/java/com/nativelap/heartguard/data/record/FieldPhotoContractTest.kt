package com.nativelap.heartguard.data.record

import com.nativelap.heartguard.data.profile.dto.ProfilePhotoUploadResponseDto
import com.nativelap.heartguard.data.profile.dto.hasMatchingContentLength
import com.nativelap.heartguard.data.record.dto.UploadFileRequestDto
import com.nativelap.heartguard.data.record.dto.UploadRequestDto
import com.nativelap.heartguard.data.record.remote.ExternalUploadService
import com.nativelap.heartguard.data.record.remote.S3UploadErrorCodeReader
import com.nativelap.heartguard.data.upload.ExactLengthPhotoBodyWriter
import com.nativelap.heartguard.data.upload.PresignedUploadHeaderValidator
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSink
import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FieldPhotoContractTest {
    @Test
    fun `presigned upload client disables connection retries`() {
        val externalUploadClient = OkHttpClient.Builder().build()
        val uploadClient = ExternalUploadService(externalUploadClient).createUploadClient()

        assertFalse(uploadClient.retryOnConnectionFailure)
    }

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

    @Test
    fun `upload headers match the file type and any signed content length`() {
        val requiredHeaders =
            mapOf(
                "content-type" to "image/jpeg",
                "Content-Length" to "3",
            )

        assertTrue(
            PresignedUploadHeaderValidator.matchesUpload(
                requiredHeaders = requiredHeaders,
                expectedContentType = "image/jpeg",
                expectedSizeBytes = 3,
                requiresContentLength = true,
            ),
        )
        assertFalse(
            PresignedUploadHeaderValidator.matchesUpload(
                requiredHeaders = requiredHeaders,
                expectedContentType = "image/png",
                expectedSizeBytes = 3,
                requiresContentLength = true,
            ),
        )
        assertFalse(
            PresignedUploadHeaderValidator.matchesUpload(
                requiredHeaders = requiredHeaders,
                expectedContentType = "image/jpeg",
                expectedSizeBytes = 4,
                requiresContentLength = true,
            ),
        )
        assertTrue(
            PresignedUploadHeaderValidator.matchesUpload(
                requiredHeaders = mapOf("Content-Type" to "image/jpeg"),
                expectedContentType = "image/jpeg",
                expectedSizeBytes = 3,
                requiresContentLength = false,
            ),
        )
        assertFalse(
            PresignedUploadHeaderValidator.matchesUpload(
                requiredHeaders =
                    mapOf(
                        "Content-Type" to "image/jpeg",
                        "content-type" to "image/jpeg",
                    ),
                expectedContentType = "image/jpeg",
                expectedSizeBytes = 3,
                requiresContentLength = false,
            ),
        )
    }

    @Test
    fun `photo body writer copies exact bytes and rejects length changes`() {
        val expectedBytes = byteArrayOf(1, 2, 3)
        val uploadedBuffer = Buffer()

        ExactLengthPhotoBodyWriter.write(
            inputStream = ByteArrayInputStream(expectedBytes),
            sink = uploadedBuffer,
            expectedSizeBytes = expectedBytes.size.toLong(),
        )

        assertContentEquals(expectedBytes, uploadedBuffer.readByteArray())
        assertFailsWith<java.io.IOException> {
            ExactLengthPhotoBodyWriter.write(
                inputStream = ByteArrayInputStream(byteArrayOf(1, 2)),
                sink = Buffer(),
                expectedSizeBytes = 3,
            )
        }
        assertFailsWith<java.io.IOException> {
            ExactLengthPhotoBodyWriter.write(
                inputStream = ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)),
                sink = Buffer(),
                expectedSizeBytes = 3,
            )
        }
    }

    @Test
    fun `presigned upload preserves URL headers and bytes without app authorization`() =
        runBlocking {
            val uploadUrl = "https://upload.example.test/object?part=1&part=2"
            val photoBytes = byteArrayOf(1, 2, 3)
            val capturedRequest = AtomicReference<Request?>()
            val externalUploadClient =
                OkHttpClient
                    .Builder()
                    .addInterceptor { chain ->
                        capturedRequest.set(chain.request())
                        Response
                            .Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body("".toResponseBody())
                            .build()
                    }.build()
            val photoRequestBody =
                object : RequestBody() {
                    override fun contentType() = null

                    override fun contentLength(): Long = photoBytes.size.toLong()

                    override fun writeTo(sink: BufferedSink) {
                        sink.write(photoBytes)
                    }
                }

            val uploadResult =
                ExternalUploadService(externalUploadClient).uploadFile(
                    uploadUrl = uploadUrl,
                    requiredHeaders =
                        mapOf(
                            "Content-Type" to "image/jpeg",
                            "Content-Length" to photoBytes.size.toString(),
                        ),
                    photoContent = photoRequestBody,
                )

            val capturedUploadRequest = requireNotNull(capturedRequest.get())
            val uploadedPhotoBytes = Buffer()
            capturedUploadRequest.body?.writeTo(uploadedPhotoBytes)

            assertEquals(200, uploadResult.statusCode)
            assertNull(uploadResult.errorCode)
            assertEquals(uploadUrl, capturedUploadRequest.url.toString())
            assertEquals("image/jpeg", capturedUploadRequest.header("Content-Type"))
            assertEquals(photoBytes.size.toString(), capturedUploadRequest.header("Content-Length"))
            assertNull(capturedUploadRequest.header("Authorization"))
            assertContentEquals(photoBytes, uploadedPhotoBytes.readByteArray())
        }

    @Test
    fun `presigned upload classifies allowlisted S3 error without returning response body`() =
        runBlocking {
            val forbiddenXml =
                """<Error><Code>InvalidAccessKeyId</Code><Message>redacted detail</Message></Error>"""
            val externalUploadClient =
                OkHttpClient
                    .Builder()
                    .addInterceptor { chain ->
                        Response
                            .Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(403)
                            .message("Forbidden")
                            .body(forbiddenXml.toResponseBody())
                            .build()
                    }.build()

            val uploadResult =
                ExternalUploadService(externalUploadClient).uploadFile(
                    uploadUrl = "https://upload.example.test/object?part=1",
                    requiredHeaders = mapOf("Content-Type" to "image/jpeg"),
                    photoContent = "abc".toRequestBody(),
                )

            assertEquals(403, uploadResult.statusCode)
            assertEquals("S3_INVALID_ACCESS_KEY_ID", uploadResult.errorCode)
            assertEquals(
                "S3_INVALID_ACCESS_KEY_ID",
                S3UploadErrorCodeReader.read(forbiddenXml),
            )
            assertNull(S3UploadErrorCodeReader.read("<Error><Code>UnknownCode</Code></Error>"))
        }
}
