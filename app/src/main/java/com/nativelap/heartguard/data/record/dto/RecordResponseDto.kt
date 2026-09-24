package com.nativelap.heartguard.data.record.dto

import kotlinx.serialization.Serializable

/** 현장 기록 등록 응답의 data 필드다.
 * 예: {"recordId":"rec_01","apparentTemperature":31.2,"heatLevel":2,"photoIds":["photo_01"]} */
@Serializable
data class RecordResponseDto(
    val recordId: String,
    val apparentTemperature: Double? = null,
    val heatLevel: Int? = null,
    val photoIds: List<String> = emptyList(),
)
