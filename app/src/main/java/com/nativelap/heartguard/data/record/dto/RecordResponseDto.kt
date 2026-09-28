package com.nativelap.heartguard.data.record.dto

import kotlinx.serialization.Serializable

/** 현장 기록 등록 응답의 data 필드다.
 * 예: {"recordId":"rec_01","apparentTemperature":31.2,"heatLevel":2,"photoIds":["photo_01"],
 * "createdAt":"2026-08-07T09:00:00+09:00"} */
@Serializable
data class RecordResponseDto(
    val recordId: String,
    val apparentTemperature: Double? = null,
    val heatLevel: Int? = null,
    val photoIds: List<String> = emptyList(),
    // 명세 응답 필드지만 응답 예시에는 빠져 있어, 없어도 저장 자체가 실패하지 않도록 nullable로 받는다.
    val createdAt: String? = null,
)
