package com.nativelap.heartguard.domain.inquiry.model

/** 작업자 문의 등록 API가 반환하는 접수 정보를 나타낸다. */
data class InquirySubmission(
    val inquiryId: String,
    val status: InquiryStatus,
    val deliveryStatus: InquiryDeliveryStatus,
    val createdAt: String,
)

enum class InquiryStatus {
    OPEN,
    UNKNOWN,
}

enum class InquiryDeliveryStatus {
    PENDING,
    UNKNOWN,
}
