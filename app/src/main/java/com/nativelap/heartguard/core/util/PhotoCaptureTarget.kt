package com.nativelap.heartguard.core.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/** CameraX가 기록할 캐시 파일과 기존 미리보기·업로드 흐름에서 사용할 URI를 함께 제공한다. */
internal data class PhotoCaptureTarget(
    val file: File,
    val uri: Uri,
)

/** 앱 캐시에 고유한 JPEG 파일을 만들고 FileProvider URI를 반환한다. 실패하면 만들어진 파일을 지운다. */
internal fun createPhotoCaptureTarget(context: Context): PhotoCaptureTarget? {
    val photoDirectory = File(context.cacheDir, PHOTO_CACHE_DIRECTORY)
    if (!photoDirectory.exists() && !photoDirectory.mkdirs()) {
        return null
    }

    val captureFile = File(
        photoDirectory,
        "$PHOTO_CAPTURE_FILE_PREFIX${UUID.randomUUID()}.jpg",
    )

    return try {
        if (!captureFile.createNewFile()) {
            return null
        }

        val captureUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            captureFile,
        )
        PhotoCaptureTarget(
            file = captureFile,
            uri = captureUri,
        )
    } catch (_: Exception) {
        captureFile.delete()
        null
    }
}
