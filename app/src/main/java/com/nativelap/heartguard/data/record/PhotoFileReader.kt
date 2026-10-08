package com.nativelap.heartguard.data.record

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.FileNotFoundException
import java.security.MessageDigest
import java.util.Base64
import javax.inject.Inject

/** 로컬 사진 Uri에서 업로드에 필요한 MIME 타입·크기·SHA-256을 스트리밍으로 계산하고, 업로드 본문을 만든다.
 * 사진 전체를 ByteArray로 올리지 않아 Photo Picker 원본(수십 MB) 2장을 동시에 다뤄도 메모리를 크게 쓰지 않는다. */
class PhotoFileReader
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        /** [uri]를 한 번 끝까지 읽어 크기와 해시를 구한다. 파일이 없거나 권한이 없으면 예외를 던진다(호출부가 로컬 파일 오류로 처리). */
        internal fun read(uri: Uri): PhotoUploadPayload {
            val messageDigest = MessageDigest.getInstance("SHA-256")
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
                    messageDigest.update(readBuffer, 0, readCount)
                    totalBytes += readCount
                }
            }

            return PhotoUploadPayload(
                photoUri = uri,
                contentType = context.contentResolver.getType(uri) ?: DEFAULT_CONTENT_TYPE,
                sizeBytes = totalBytes,
                sha256 = Base64.getEncoder().encodeToString(messageDigest.digest()),
            )
        }

        /** presigned URL PUT 본문이다. OkHttp가 전송할 때 Uri에서 바로 스트리밍하며, 재시도 시에도 다시 열어 읽는다.
         * 본문의 contentType을 null로 둔다. 값이 있으면 OkHttp가 Content-Type 헤더를 그 값으로 덮어써,
         * 서버가 서명에 포함한 requiredHeaders의 Content-Type과 달라질 수 있기 때문이다. 헤더는 호출부가 붙인다. */
        internal fun createUploadBody(payload: PhotoUploadPayload): RequestBody {
            val contentResolver = context.contentResolver
            return object : RequestBody() {
                override fun contentType(): MediaType? = null

                override fun contentLength(): Long = payload.sizeBytes

                override fun writeTo(sink: BufferedSink) {
                    val inputStream =
                        contentResolver.openInputStream(payload.photoUri)
                            ?: throw FileNotFoundException("사진 파일을 열 수 없습니다: ${payload.photoUri}")
                    inputStream.source().use { photoSource ->
                        sink.writeAll(photoSource)
                    }
                }
            }
        }

        private companion object {
            const val DEFAULT_CONTENT_TYPE = "image/jpeg"
            const val READ_BUFFER_SIZE = 64 * 1024
        }
    }
