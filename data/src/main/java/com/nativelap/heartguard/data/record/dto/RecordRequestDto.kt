package com.nativelap.heartguard.data.record.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** POST /api/v1/team/records 요청 본문이다.
 * 예: {"type":"WORK","photoKeys":["teams/team_01/uploads/up_01.jpg"],"memo":"오전 작업",
 * "measuredAt":"2026-08-07T09:00:00+09:00"} */
@Serializable
data class RecordRequestDto(
    @SerialName("type")
    val type: String,
    @SerialName("photoKeys")
    val photoKeys: List<String>,
    @SerialName("temperature")
    val temperature: Double? = null,
    @SerialName("humidity")
    val humidity: Double? = null,
    @SerialName("memo")
    val memo: String? = null,
    @SerialName("measuredAt")
    val measuredAt: String,
    @SerialName("restStartedAt")
    val restStartedAt: String? = null,
    @SerialName("restEndedAt")
    val restEndedAt: String? = null,
)
