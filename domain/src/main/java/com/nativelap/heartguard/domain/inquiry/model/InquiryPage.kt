package com.nativelap.heartguard.domain.inquiry.model

/** 내 문의 목록의 한 페이지다. [nextCursor]가 null이면 마지막 페이지다. */
data class InquiryPage(
    val inquiries: List<InquirySummary>,
    val nextCursor: String?,
)
