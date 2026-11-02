package com.nativelap.heartguard.core.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File

/** 카메라로 촬영한 임시 파일과 갤러리에서 선택한 사진을 정리하는 공통 로직이다.
 * [com.nativelap.heartguard.view.route.photo.PhotoSelectionFlow](화면이 떠 있는 동안의 촬영·선택)와
 * [com.nativelap.heartguard.viewmodel.record.RecordDraftViewModel](기록 흐름 전체가 끝났을 때의 일괄 정리)이 함께 쓴다. */

internal const val PHOTO_CAPTURE_FILE_PREFIX = "heartguard_capture_"
internal const val PHOTO_CACHE_DIRECTORY = "photos"

// file_paths.xml의 cache-path name과 같아야 FileProvider URI의 첫 경로 세그먼트가 맞는다.
private const val PHOTO_CAPTURE_PROVIDER_ROOT = "photo-captures"

// FileProvider URI는 `<루트>/<파일 이름>` 두 세그먼트로만 이루어진다.
private const val PHOTO_CAPTURE_PATH_SEGMENT_COUNT = 2

// PhotoCaptureTarget이 만드는 이름(prefix + UUID.randomUUID() + .jpg)과 같은 형식이다. UUID 문자열은 소문자다.
private val PHOTO_CAPTURE_FILE_NAME_PATTERN =
    Regex("${PHOTO_CAPTURE_FILE_PREFIX}[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg")

internal fun isCameraCaptureUri(
    context: Context,
    uri: Uri,
): Boolean = uri.authority == "${context.packageName}.fileprovider"

/** 갤러리에서 선택한 사진은 부여받은 읽기 권한을 해제하고, 카메라로 촬영한 임시 파일은 캐시에서 삭제한다. */
internal fun releasePhoto(
    context: Context,
    uri: Uri,
) {
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

/** 촬영 파일 이름이 앱이 만든 형식과 정확히 같은지 확인한다. */
internal fun isCaptureFileName(fileName: String): Boolean = PHOTO_CAPTURE_FILE_NAME_PATTERN.matches(fileName)

/** FileProvider URI의 경로 세그먼트가 `photo-captures/<촬영 파일 이름>` 구조일 때만 촬영 파일 이름을 돌려준다. */
internal fun captureFileNameFrom(pathSegments: List<String>): String? {
    if (pathSegments.size != PHOTO_CAPTURE_PATH_SEGMENT_COUNT ||
        pathSegments.first() != PHOTO_CAPTURE_PROVIDER_ROOT
    ) {
        return null
    }

    return pathSegments.last().takeIf(::isCaptureFileName)
}

/** 후보 파일의 실제 경로(심볼릭 링크를 해석한 뒤)가 디렉터리 바로 아래에 있을 때만 true를 반환한다. */
internal fun isDirectChildOf(
    directory: File,
    candidate: File,
): Boolean {
    val canonicalDirectory = directory.canonicalFile
    return candidate.canonicalFile.parentFile == canonicalDirectory
}

/** 앱이 만든 촬영 파일만 캐시 사진 디렉터리에서 삭제한다. 형식이나 위치가 맞지 않으면 아무것도 지우지 않는다. */
internal fun deleteCaptureFile(
    context: Context,
    uri: Uri,
) {
    if (!isCameraCaptureUri(context, uri)) {
        return
    }
    if (uri.scheme != ContentResolver.SCHEME_CONTENT) {
        return
    }

    val fileName = captureFileNameFrom(uri.pathSegments) ?: return
    val photoDirectory = File(context.cacheDir, PHOTO_CACHE_DIRECTORY)
    val captureFile = File(photoDirectory, fileName)
    if (!isDirectChildOf(photoDirectory, captureFile)) {
        return
    }

    captureFile.delete()
}
