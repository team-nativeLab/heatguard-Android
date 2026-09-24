package com.nativelap.heartguard.data.record

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.util.Base64
import javax.inject.Inject

/** 로컬 사진 Uri에서 업로드에 필요한 바이트·MIME 타입·SHA-256 해시를 읽어 낸다.
 * 사진 업로드 URL 발급 요청(files[].contentType/size/sha256)을 만들기 위해 필요하다. */
class PhotoFileReader @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    internal fun read(uri: Uri): PhotoUploadPayload {
        val bytes = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        } ?: error("사진 파일을 읽을 수 없습니다: $uri")

        val contentType = context.contentResolver.getType(uri) ?: DEFAULT_CONTENT_TYPE
        val sha256 = Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(bytes),
        )

        return PhotoUploadPayload(
            contentType = contentType,
            sizeBytes = bytes.size.toLong(),
            sha256 = sha256,
            bytes = bytes,
        )
    }

    private companion object {
        const val DEFAULT_CONTENT_TYPE = "image/jpeg"
    }
}
