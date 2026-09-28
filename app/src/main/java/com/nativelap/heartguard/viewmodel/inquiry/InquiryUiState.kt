package com.nativelap.heartguard.viewmodel.inquiry

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.domain.inquiry.model.InquirySubmission

data class InquiryUiState(
    val title: String = "",
    val content: String = "",
    val isSubmitting: Boolean = false,
    val submissionError: ApiError? = null,
    val submittedInquiry: InquirySubmission? = null,
) {
    val canSubmit: Boolean
        get() = title.isNotBlank() && content.isNotBlank() && !isSubmitting
}
