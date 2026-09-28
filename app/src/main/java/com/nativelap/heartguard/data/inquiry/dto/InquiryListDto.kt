package com.nativelap.heartguard.data.inquiry.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** GET /api/v1/team/inquiries 응답의 data 필드다. 명세에 항목 정의가 없어 2026-09-28 실서버 응답으로 확인한 구조다. */
@Serializable
data class InquiryListPageDto(
    @SerialName("items")
    val items: List<InquiryListItemDto> = emptyList(),
    @SerialName("page")
    val page: InquiryListCursorDto? = null,
)

@Serializable
data class InquiryListCursorDto(
    @SerialName("nextCursor")
    val nextCursor: String? = null,
    @SerialName("hasMore")
    val hasMore: Boolean = false,
)

/** 문의 목록 항목이다. 예: {"inquiryId":"inq_01","title":"제목","content":"내용","status":"OPEN",
 * "deliveryStatus":"PENDING","replies":[],"createdAt":"2026-09-28T14:42:05+00:00"}
 * 답변 본문은 문의 상세 화면이 범위 밖이라 개수만 쓰며, 구조가 정의되지 않아 JSON 그대로 받는다. */
@Serializable
data class InquiryListItemDto(
    @SerialName("inquiryId")
    val inquiryId: String,
    @SerialName("title")
    val title: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("replies")
    val replies: List<JsonElement> = emptyList(),
    @SerialName("createdAt")
    val createdAt: String? = null,
)
