package com.nativelap.heartguard.domain.inquiry.model

import java.time.OffsetDateTime

/** 내 문의 목록의 문의 한 건이다. [createdAt]을 해석할 수 없으면 null이다. */
data class InquirySummary(
    val inquiryId: String,
    val title: String?,
    val status: InquiryStatus,
    val replyCount: Int,
    val createdAt: OffsetDateTime?,
)

/** 문의 처리 상태다. 서버가 새 상태를 추가해도 목록이 실패하지 않도록 [UNKNOWN]을 둔다. */
enum class InquiryStatus {
    OPEN,
    ANSWERED,
    CLOSED,
    UNKNOWN,
}
