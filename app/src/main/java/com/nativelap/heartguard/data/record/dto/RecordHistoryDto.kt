package com.nativelap.heartguard.data.record.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** GET /api/v1/team/records 응답의 data 필드다. 커서 기반 페이지 정보가 함께 온다. */
@Serializable
data class RecordHistoryPageDto(
    @SerialName("items")
    val items: List<RecordHistoryItemDto> = emptyList(),
    @SerialName("page")
    val page: RecordHistoryCursorDto? = null,
)

@Serializable
data class RecordHistoryCursorDto(
    @SerialName("nextCursor")
    val nextCursor: String? = null,
    @SerialName("hasMore")
    val hasMore: Boolean = false,
)

/** 기록 목록 항목이자 GET /api/v1/team/records/{recordId} 응답의 data 필드다.
 * 예: {"recordId":"rec_01","type":"THERMOMETER","temperature":31.5,"humidity":60.0,"apparentTemperature":35.2,
 * "heatLevel":3,"photoKeys":["teams/team_01/uploads/up_01.jpg"],"memo":"메모","measuredAt":"2026-09-28T23:18:05+09:00",
 * "createdAt":"2026-09-28T14:18:05+00:00","photoUrls":["https://..."]}
 * 2026-09-28 실서버 응답으로 확인한 구조에 명세 v0.1(2026-09-29)의 restMinutes·teamName·workplace·siteName을 더했다.
 * 위치·팀명은 기록 당시 값이고, 이 필드가 생기기 전 기록은 null이다. photoUrls는 상세 응답에만 있다.
 * type은 서버가 새 값을 추가해도 목록 전체가 실패하지 않도록 문자열로 받고 mapper에서 해석한다. */
@Serializable
data class RecordHistoryItemDto(
    @SerialName("recordId")
    val recordId: String,
    @SerialName("type")
    val type: String? = null,
    @SerialName("temperature")
    val temperature: Double? = null,
    @SerialName("humidity")
    val humidity: Double? = null,
    @SerialName("apparentTemperature")
    val apparentTemperature: Double? = null,
    @SerialName("heatLevel")
    val heatLevel: Int? = null,
    @SerialName("photoKeys")
    val photoKeys: List<String> = emptyList(),
    @SerialName("photoUrls")
    val photoUrls: List<String> = emptyList(),
    @SerialName("memo")
    val memo: String? = null,
    @SerialName("measuredAt")
    val measuredAt: String? = null,
    @SerialName("createdAt")
    val createdAt: String? = null,
    // 휴식(REST) 기록의 휴식 시간(분)이다.
    @SerialName("restMinutes")
    val restMinutes: Int? = null,
    @SerialName("teamName")
    val teamName: String? = null,
    @SerialName("workplace")
    val workplace: String? = null,
    @SerialName("siteName")
    val siteName: String? = null,
)
