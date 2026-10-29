package com.nativelap.heartguard.data.profile

import android.content.Context
import android.net.Uri
import com.nativelap.heartguard.data.upload.ExactLengthPhotoBodyWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.FileNotFoundException
import javax.inject.Inject

/** Validates a picked image and exposes a streaming request body without retaining its bytes. */
class ProfilePhotoFileReader
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        fun read(uri: Uri): ProfilePhotoUploadPayload {
            val detectedMimeType = detectMimeType(uri)
            require(detectedMimeType in ALLOWED_MIME_TYPES) { "Unsupported profile photo type" }

            val declaredMimeType = context.contentResolver.getType(uri)
            require(declaredMimeType == null || declaredMimeType == detectedMimeType) {
                "Profile photo type does not match its contents"
            }

            val fileSize = countBytes(uri)
            require(fileSize in MINIMUM_PHOTO_BYTES..MAXIMUM_PHOTO_BYTES) {
                "Profile photo size is outside the allowed range"
            }

            return ProfilePhotoUploadPayload(
                uri = uri,
                contentType = detectedMimeType,
                sizeBytes = fileSize,
            )
        }

        fun createRequestBody(payload: ProfilePhotoUploadPayload): RequestBody {
            val contentResolver = context.contentResolver
            return object : RequestBody() {
                override fun contentType(): MediaType? = null

                override fun contentLength(): Long = payload.sizeBytes

                override fun writeTo(sink: BufferedSink) {
                    val inputStream =
                        contentResolver.openInputStream(payload.uri)
                            ?: throw FileNotFoundException("Profile photo could not be opened")
                    inputStream.use { photoStream ->
                        ExactLengthPhotoBodyWriter.write(
                            inputStream = photoStream,
                            sink = sink,
                            expectedSizeBytes = payload.sizeBytes,
                        )
                    }
                }
            }
        }

        private fun detectMimeType(uri: Uri): String {
            val photoStream =
                context.contentResolver.openInputStream(uri)
                    ?: throw FileNotFoundException("Profile photo could not be opened")
            val headerBytes =
                photoStream.use { inputStream ->
                    val header = ByteArray(12)
                    var offset = 0
                    while (offset < header.size) {
                        val readCount = inputStream.read(header, offset, header.size - offset)
                        if (readCount < 0) {
                            break
                        }
                        offset += readCount
                    }
                    header.copyOf(offset)
                }

            return when {
                headerBytes.startsWith(PNG_SIGNATURE) -> MIME_PNG

                headerBytes.startsWith(JPEG_SIGNATURE) -> MIME_JPEG

                headerBytes.size >= 12 &&
                    headerBytes.startsWith(WEBP_RIFF_SIGNATURE) &&
                    headerBytes.copyOfRange(8, 12).contentEquals(WEBP_SIGNATURE) -> MIME_WEBP

                else -> ""
            }
        }

        private fun countBytes(uri: Uri): Long {
            val photoStream =
                context.contentResolver.openInputStream(uri)
                    ?: throw FileNotFoundException("Profile photo could not be opened")
            photoStream.use { inputStream ->
                val buffer = ByteArray(BUFFER_SIZE)
                var byteCount = 0L
                while (true) {
                    val readCount = inputStream.read(buffer)
                    if (readCount < 0) {
                        break
                    }
                    byteCount += readCount
                    if (byteCount > MAXIMUM_PHOTO_BYTES) {
                        return byteCount
                    }
                }
                return byteCount
            }
        }

        private companion object {
            const val MIME_JPEG = "image/jpeg"
            const val MIME_PNG = "image/png"
            const val MIME_WEBP = "image/webp"
            const val MINIMUM_PHOTO_BYTES = 1L
            const val MAXIMUM_PHOTO_BYTES = 5L * 1024L * 1024L
            const val BUFFER_SIZE = 16 * 1024
            val ALLOWED_MIME_TYPES = setOf(MIME_JPEG, MIME_PNG, MIME_WEBP)
            val JPEG_SIGNATURE = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
            val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
            val WEBP_RIFF_SIGNATURE = byteArrayOf(0x52, 0x49, 0x46, 0x46)
            val WEBP_SIGNATURE = byteArrayOf(0x57, 0x45, 0x42, 0x50)
        }
    }

data class ProfilePhotoUploadPayload(
    val uri: Uri,
    val contentType: String,
    val sizeBytes: Long,
)

private fun ByteArray.startsWith(signature: ByteArray): Boolean =
    size >= signature.size && signature.indices.all { index -> this[index] == signature[index] }
