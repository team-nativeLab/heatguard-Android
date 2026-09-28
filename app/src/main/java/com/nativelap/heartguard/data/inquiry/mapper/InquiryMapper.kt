package com.nativelap.heartguard.data.inquiry.mapper

import com.nativelap.heartguard.data.inquiry.dto.SubmitInquiryResponseDto
import com.nativelap.heartguard.domain.inquiry.model.InquiryDeliveryStatus
import com.nativelap.heartguard.domain.inquiry.model.InquirySubmission
import com.nativelap.heartguard.domain.inquiry.model.InquiryStatus

internal fun SubmitInquiryResponseDto.toDomain(): InquirySubmission = InquirySubmission(
    inquiryId = inquiryId,
    status = status.toInquiryStatus(),
    deliveryStatus = deliveryStatus.toInquiryDeliveryStatus(),
    createdAt = createdAt,
)

private fun String.toInquiryStatus(): InquiryStatus = when (this) {
    "OPEN" -> InquiryStatus.OPEN
    else -> InquiryStatus.UNKNOWN
}

private fun String.toInquiryDeliveryStatus(): InquiryDeliveryStatus = when (this) {
    "PENDING" -> InquiryDeliveryStatus.PENDING
    else -> InquiryDeliveryStatus.UNKNOWN
}
