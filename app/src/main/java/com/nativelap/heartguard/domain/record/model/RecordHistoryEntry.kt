package com.nativelap.heartguard.domain.record.model

import java.time.OffsetDateTime

/** 작업자가 저장한 기록 한 건이다. 목록과 상세가 함께 쓰며, [photoUrls]는 상세 조회에서만 채워진다.
 * [type]은 앱이 모르는 기록 유형이면 null이다. 서버가 주지 않은 값은 null이다.
 * [teamName]·[workplace]·[siteName]은 기록을 저장한 당시의 값이다. */
data class RecordHistoryEntry(
    val recordId: String,
    val type: FieldRecordType?,
    val temperature: Double?,
    val humidity: Double?,
    val apparentTemperature: Double?,
    val heatLevel: Int?,
    val photoCount: Int,
    val photoUrls: List<String>,
    val memo: String?,
    val measuredAt: OffsetDateTime?,
    val restMinutes: Int? = null,
    val teamName: String? = null,
    val workplace: String? = null,
    val siteName: String? = null,
)
