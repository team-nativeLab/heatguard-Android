package com.nativelap.heartguard.data.record.dto

import kotlinx.serialization.Serializable

/** 사진 업로드 URL 발급 요청 본문이다.
 * 예: {"files":[{"slot":1,"contentType":"image/jpeg","size":1843200,"sha256":"base64-sha256"}]} */
@Serializable
data class UploadRequestDto(
    val files: List<UploadFileRequestDto>,
)

@Serializable
data class UploadFileRequestDto(
    val slot: Int,
    val contentType: String,
    val size: Long,
    val sha256: String,
)
