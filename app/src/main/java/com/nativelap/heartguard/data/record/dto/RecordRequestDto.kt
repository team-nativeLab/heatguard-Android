package com.nativelap.heartguard.data.record.dto

import kotlinx.serialization.Serializable

/** POST /api/v1/t/{teamToken}/records 요청 본문이다.
 * 예: {"type":"WORK","photoKeys":["teams/team_01/uploads/up_01.jpg"],"memo":"오전 작업",
 * "measuredAt":"2026-08-07T09:00:00+09:00"} */
@Serializable
data class RecordRequestDto(
    val type: String,
    val photoKeys: List<String>,
    val temperature: Double? = null,
    val humidity: Double? = null,
    val noThermometer: Boolean? = null,
    val memo: String? = null,
    val measuredAt: String,
)
