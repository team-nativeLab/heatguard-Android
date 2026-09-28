package com.nativelap.heartguard.data.inquiry.mapper

import com.nativelap.heartguard.data.inquiry.dto.InquirySubmissionResponseDto
import com.nativelap.heartguard.domain.inquiry.model.InquirySubmission

internal fun InquirySubmissionResponseDto.toDomain(): InquirySubmission = InquirySubmission(
    inquiryId = inquiryId,
    status = status,
    deliveryStatus = deliveryStatus,
    createdAt = createdAt,
)
