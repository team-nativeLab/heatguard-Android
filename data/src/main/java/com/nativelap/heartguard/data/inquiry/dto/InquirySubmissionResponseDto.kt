package com.nativelap.heartguard.data.inquiry.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** POST /api/v1/team/inquiries 응답의 data 필드다. */
@Serializable
data class InquirySubmissionResponseDto(
    @SerialName("inquiryId")
    val inquiryId: String,
    @SerialName("status")
    val status: String,
    @SerialName("deliveryStatus")
    val deliveryStatus: String,
    @SerialName("createdAt")
    val createdAt: String,
)
