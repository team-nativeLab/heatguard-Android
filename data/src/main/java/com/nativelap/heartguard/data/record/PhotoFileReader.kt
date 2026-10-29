package com.nativelap.heartguard.data.record

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import javax.inject.Inject

/** 로컬 사진 Uri에서 실제 MIME 타입과 바이트 크기를 검증하고 업로드 본문을 만든다. */
class PhotoFileReader
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        /** 사진 내용을 메모리에 유지하지 않고 크기와 시그니처를 스트리밍으로 검증한다. */
        internal fun read(uri: Uri): PhotoUploadPayload {
            val headerBytes = ByteArray(FieldPhotoValidation.IMAGE_HEADER_BYTE_COUNT)
            var headerByteCount = 0
            var totalBytes = 0L
            val inputStream =
                context.contentResolver.openInputStream(uri)
                    ?: throw FileNotFoundException("사진 파일을 열 수 없습니다: $uri")
            inputStream.use { photoStream ->
                val readBuffer = ByteArray(READ_BUFFER_SIZE)
                while (true) {
                    val readCount = photoStream.read(readBuffer)
                    if (readCount < 0) {
                        break
                    }

                    totalBytes += readCount
                    require(FieldPhotoValidation.isSupportedSize(totalBytes)) {
                        "사진 크기는 1바이트 이상 5MiB 이하여야 합니다"
                    }

                    val bytesToCopy = minOf(readCount, headerBytes.size - headerByteCount)
                    if (bytesToCopy > 0) {
                        readBuffer.copyInto(
                            destination = headerBytes,
                            destinationOffset = headerByteCount,
                            startIndex = 0,
                            endIndex = bytesToCopy,
                        )
                        headerByteCount += bytesToCopy
                    }
                }
            }
            require(FieldPhotoValidation.isSupportedSize(totalBytes)) {
                "사진 크기는 1바이트 이상 5MiB 이하여야 합니다"
            }

            val detectedMimeType =
                FieldPhotoValidation.detectMimeType(headerBytes.copyOf(headerByteCount))
                    ?: throw IllegalArgumentException("JPEG, PNG 또는 WebP 사진만 업로드할 수 있습니다")
            val declaredMimeType = context.contentResolver.getType(uri)
            require(declaredMimeType == null || declaredMimeType == detectedMimeType) {
                "사진 형식과 MIME 타입이 일치하지 않습니다"
            }

            return PhotoUploadPayload(
                photoUri = uri,
                contentType = detectedMimeType,
                sizeBytes = totalBytes,
            )
        }

        /** 업로드 시 파일 크기가 검증 후 달라지면, 서명과 Content-Length가 어긋나는 요청을 중단한다. */
        internal fun createUploadBody(payload: PhotoUploadPayload): RequestBody {
            val contentResolver = context.contentResolver
            return object : RequestBody() {
                override fun contentType(): MediaType? = null

                override fun contentLength(): Long = payload.sizeBytes

                override fun writeTo(sink: BufferedSink) {
                    val inputStream =
                        contentResolver.openInputStream(payload.photoUri)
                            ?: throw FileNotFoundException("사진 파일을 열 수 없습니다: ${payload.photoUri}")
                    inputStream.use { photoStream ->
                        val readBuffer = ByteArray(READ_BUFFER_SIZE)
                        var writtenBytes = 0L
                        while (true) {
                            val readCount = photoStream.read(readBuffer)
                            if (readCount < 0) {
                                break
                            }
                            if (writtenBytes + readCount > payload.sizeBytes) {
                                throw IOException("사진 파일 크기가 업로드 요청 이후 변경되었습니다")
                            }
                            sink.write(readBuffer, 0, readCount)
                            writtenBytes += readCount
                        }
                        if (writtenBytes != payload.sizeBytes) {
                            throw IOException("사진 파일 크기가 업로드 요청 이후 변경되었습니다")
                        }
                    }
                }
            }
        }

        private companion object {
            const val READ_BUFFER_SIZE = 64 * 1024
        }
    }

internal object FieldPhotoValidation {
    const val IMAGE_HEADER_BYTE_COUNT = 12
    private const val MINIMUM_PHOTO_BYTES = 1L
    private const val MAXIMUM_PHOTO_BYTES = 5L * 1024L * 1024L
    private const val MAXIMUM_FIELD_PHOTO_COUNT = 2
    private const val HTTPS_SCHEME = "https"
    private const val MIME_JPEG = "image/jpeg"
    private const val MIME_PNG = "image/png"
    private const val MIME_WEBP = "image/webp"
    private val JPEG_SIGNATURE = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private val WEBP_RIFF_SIGNATURE = byteArrayOf(0x52, 0x49, 0x46, 0x46)
    private val WEBP_SIGNATURE = byteArrayOf(0x57, 0x45, 0x42, 0x50)

    fun isSupportedSize(sizeBytes: Long): Boolean = sizeBytes in MINIMUM_PHOTO_BYTES..MAXIMUM_PHOTO_BYTES

    fun isAllowedUploadCount(photoCount: Int): Boolean = photoCount in 1..MAXIMUM_FIELD_PHOTO_COUNT

    fun isUploadUsableAt(
        uploadUrl: String,
        expiresAt: String,
        now: Instant,
    ): Boolean {
        val uploadUri =
            try {
                URI(uploadUrl)
            } catch (_: URISyntaxException) {
                return false
            }
        if (uploadUri.scheme?.equals(HTTPS_SCHEME, ignoreCase = true) != true || uploadUri.host.isNullOrBlank()) {
            return false
        }

        val expiryInstant =
            try {
                OffsetDateTime.parse(expiresAt).toInstant()
            } catch (_: DateTimeParseException) {
                return false
            }
        return expiryInstant.isAfter(now)
    }

    fun detectMimeType(headerBytes: ByteArray): String? =
        when {
            headerBytes.startsWith(JPEG_SIGNATURE) -> MIME_JPEG

            headerBytes.startsWith(PNG_SIGNATURE) -> MIME_PNG

            headerBytes.size >= IMAGE_HEADER_BYTE_COUNT &&
                headerBytes.startsWith(WEBP_RIFF_SIGNATURE) &&
                headerBytes.copyOfRange(8, IMAGE_HEADER_BYTE_COUNT).contentEquals(WEBP_SIGNATURE) -> MIME_WEBP

            else -> null
        }
}

private fun ByteArray.startsWith(signature: ByteArray): Boolean =
    size >= signature.size && signature.indices.all { index -> this[index] == signature[index] }
