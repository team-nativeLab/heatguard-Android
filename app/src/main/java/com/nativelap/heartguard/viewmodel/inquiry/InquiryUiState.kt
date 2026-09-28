package com.nativelap.heartguard.viewmodel.inquiry

import com.nativelap.heartguard.domain.inquiry.model.InquirySubmission

data class InquiryUiState(
    val title: String = "",
    val content: String = "",
    val isSubmitting: Boolean = false,
    val submission: InquirySubmission? = null,
    val hasError: Boolean = false,
)
