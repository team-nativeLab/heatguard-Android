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

    /** [nextCursor]가 null이면 끝까지 받았다. 이어 받기 실패는 받은 목록을 유지한 채 [hasLoadMoreError]로 알린다. */
    data class Loaded(
        val inquiries: List<InquirySummary>,
        val nextCursor: String? = null,
        val isLoadingMore: Boolean = false,
        val hasLoadMoreError: Boolean = false,
    ) : InquiryListState

    data object Failed : InquiryListState
}
