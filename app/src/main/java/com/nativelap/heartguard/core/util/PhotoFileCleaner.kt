package com.nativelap.heartguard.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File

/** 카메라로 촬영한 임시 파일과 갤러리에서 선택한 사진을 정리하는 공통 로직이다.
 * [com.nativelap.heartguard.view.route.photo.PhotoSelectionFlow](화면이 떠 있는 동안의 촬영·선택)와
 * [com.nativelap.heartguard.viewmodel.record.RecordDraftViewModel](기록 흐름 전체가 끝났을 때의 일괄 정리)이 함께 쓴다. */

internal const val PHOTO_CAPTURE_FILE_PREFIX = "heartguard_capture_"
internal const val PHOTO_CACHE_DIRECTORY = "photos"

internal fun isCameraCaptureUri(context: Context, uri: Uri): Boolean =
    uri.authority == "${context.packageName}.fileprovider"

/** 갤러리에서 선택한 사진은 부여받은 읽기 권한을 해제하고, 카메라로 촬영한 임시 파일은 캐시에서 삭제한다. */
internal fun releasePhoto(context: Context, uri: Uri) {
    if (isCameraCaptureUri(context, uri)) {
        deleteCaptureFile(context, uri)
        return
    }

    try {
        context.contentResolver.releasePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    } catch (_: SecurityException) {
        // 임시 권한만 받은 선택 항목은 해제 가능한 영속 권한이 없다.
    }
}

internal fun deleteCaptureFile(context: Context, uri: Uri) {
    if (!isCameraCaptureUri(context, uri)) {
        return
    }

    val fileName = uri.lastPathSegment ?: return
    if (!fileName.startsWith(PHOTO_CAPTURE_FILE_PREFIX)) {
        return
    }

    File(File(context.cacheDir, PHOTO_CACHE_DIRECTORY), fileName).delete()
}
