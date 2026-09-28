package com.nativelap.heartguard.viewmodel.inquiry

import com.nativelap.heartguard.domain.inquiry.model.InquirySubmission
import com.nativelap.heartguard.domain.inquiry.model.InquirySummary

/** 문의하기 화면 상태다. 문의 입력·등록 상태와 내 문의 목록 조회 결과를 함께 가진다. */
data class InquiryUiState(
    val title: String = "",
    val content: String = "",
    val isSubmitting: Boolean = false,
    val submission: InquirySubmission? = null,
    val hasError: Boolean = false,
    val listState: InquiryListState = InquiryListState.Loading,
)

/** 내 문의 목록 조회 결과다. */
sealed interface InquiryListState {
    data object Loading : InquiryListState

    data class Loaded(val inquiries: List<InquirySummary>) : InquiryListState

    data object Failed : InquiryListState
}
