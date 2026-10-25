package com.nativelap.heartguard.data.inquiry.mapper

import com.nativelap.heartguard.data.inquiry.dto.InquiryListItemDto
import com.nativelap.heartguard.data.inquiry.dto.InquirySubmissionResponseDto
import com.nativelap.heartguard.domain.inquiry.model.InquiryStatus
import com.nativelap.heartguard.domain.inquiry.model.InquirySubmission
import com.nativelap.heartguard.domain.inquiry.model.InquirySummary
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

internal fun InquirySubmissionResponseDto.toDomain(): InquirySubmission =
    InquirySubmission(
        inquiryId = inquiryId,
        status = status,
        deliveryStatus = deliveryStatus,
        createdAt = createdAt,
    )

internal fun InquiryListItemDto.toDomain(): InquirySummary =
    InquirySummary(
        inquiryId = inquiryId,
        title = title?.takeIf { inquiryTitle -> inquiryTitle.isNotBlank() },
        status = status.toInquiryStatus(),
        replyCount = replies.size,
        createdAt = createdAt?.toOffsetDateTimeOrNull(),
    )

private fun String?.toInquiryStatus(): InquiryStatus =
    when (this) {
        "OPEN" -> InquiryStatus.OPEN
        "ANSWERED" -> InquiryStatus.ANSWERED
        "CLOSED" -> InquiryStatus.CLOSED
        else -> InquiryStatus.UNKNOWN
    }

private fun String.toOffsetDateTimeOrNull(): OffsetDateTime? =
    try {
        OffsetDateTime.parse(this)
    } catch (_: DateTimeParseException) {
        null
    }
