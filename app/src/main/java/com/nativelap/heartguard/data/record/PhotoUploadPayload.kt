package com.nativelap.heartguard.data.record

import android.net.Uri

/** 업로드 URL 발급 요청(files[].contentType/size/sha256)에 필요한 사진 정보다.
 * 원본 바이트는 메모리에 들고 있지 않고, 실제 업로드 때 [photoUri]에서 다시 스트리밍한다(큰 원본 사진의 OOM 방지). */
internal data class PhotoUploadPayload(
    val photoUri: Uri,
    val contentType: String,
    val sizeBytes: Long,
    val sha256: String,
)
