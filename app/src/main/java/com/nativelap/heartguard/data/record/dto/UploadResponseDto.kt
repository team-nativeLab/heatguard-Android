package com.nativelap.heartguard.data.record.dto

import kotlinx.serialization.Serializable

/** 사진 업로드 URL 발급 응답의 data 필드다.
 * 예: {"uploads":[{"objectKey":"teams/team_01/uploads/up_01.jpg","uploadUrl":"https://s3/...",
 * "requiredHeaders":{"Content-Type":"image/jpeg"},"expiresAt":"2026-08-07T09:05:00+09:00"}]} */
@Serializable
data class UploadResponseDto(
    val uploads: List<UploadSlotDto>,
)

@Serializable
data class UploadSlotDto(
    val objectKey: String,
    val uploadUrl: String,
    val requiredHeaders: Map<String, String> = emptyMap(),
    val expiresAt: String? = null,
)
