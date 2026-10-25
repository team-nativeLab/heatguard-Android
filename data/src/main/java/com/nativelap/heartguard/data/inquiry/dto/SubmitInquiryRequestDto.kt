package com.nativelap.heartguard.data.inquiry.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** POST /api/v1/team/inquiries 요청 본문이다. */
@Serializable
data class SubmitInquiryRequestDto(
    @SerialName("title")
    val title: String,
    @SerialName("content")
    val content: String,
)
