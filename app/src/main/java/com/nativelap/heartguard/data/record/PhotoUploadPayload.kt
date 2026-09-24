package com.nativelap.heartguard.data.record

/** 로컬 사진 파일에서 업로드에 필요한 값을 미리 읽어 둔 결과다. */
internal data class PhotoUploadPayload(
    val contentType: String,
    val sizeBytes: Long,
    val sha256: String,
    val bytes: ByteArray,
)
