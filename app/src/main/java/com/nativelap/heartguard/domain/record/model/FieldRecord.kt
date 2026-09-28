package com.nativelap.heartguard.domain.record.model

import java.time.OffsetDateTime

/** 현장 기록 등록(POST /api/v1/team/records) 응답을 정리한 도메인 모델이다. */
data class FieldRecord(
    val recordId: String,
    val apparentTemperature: Double?,
    val heatLevel: Int?,
    val photoIds: List<String>,
    // 서버가 기록을 저장한 시각이다. 응답에 없거나 형식을 해석할 수 없으면 null이다.
    val createdAt: OffsetDateTime?,
)
