package com.nativelap.heartguard.data.record.dto

import kotlinx.serialization.Serializable

/** 사진 업로드 URL 발급 요청 본문이다. 예: {"files":[{"contentType":"image/jpeg","size":1843200}]} */
@Serializable
data class UploadRequestDto(
    val files: List<UploadFileRequestDto>,
)

@Serializable
data class UploadFileRequestDto(
    val contentType: String,
    val size: Long,
)
