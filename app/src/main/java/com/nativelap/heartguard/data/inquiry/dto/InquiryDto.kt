package com.nativelap.heartguard.data.inquiry.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class SubmitInquiryRequestDto(
    @SerialName("title")
    val title: String,
    @SerialName("content")
    val content: String,
)

@Serializable
data class SubmitInquiryResponseDto(
    @SerialName("inquiryId")
    val inquiryId: String,
    @SerialName("status")
    val status: String,
    @SerialName("deliveryStatus")
    val deliveryStatus: String,
    @SerialName("createdAt")
    val createdAt: String,
)
