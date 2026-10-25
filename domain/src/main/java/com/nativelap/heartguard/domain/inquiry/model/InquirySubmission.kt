package com.nativelap.heartguard.domain.inquiry.model

/** 작업자가 문의를 등록한 뒤 서버에서 확인한 접수 상태다. */
data class InquirySubmission(
    val inquiryId: String,
    val status: String,
    val deliveryStatus: String,
    val createdAt: String,
)
