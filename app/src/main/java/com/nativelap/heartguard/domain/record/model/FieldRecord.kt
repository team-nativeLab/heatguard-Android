package com.nativelap.heartguard.domain.record.model

/** 현장 기록 등록(POST /api/v1/t/{teamToken}/records) 응답을 정리한 도메인 모델이다. */
data class FieldRecord(
    val recordId: String,
    val apparentTemperature: Double?,
    val heatLevel: Int?,
    val photoIds: List<String>,
)
