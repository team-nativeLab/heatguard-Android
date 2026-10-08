package com.nativelap.heartguard.viewmodel.inquiry

/** 문의 화면에서 발생하는 사용자 의도를 Route에 전달한다. */
sealed interface InquiryScreenEvent {
    data object BackClicked : InquiryScreenEvent

    data class TitleChanged(
        val title: String,
    ) : InquiryScreenEvent

    data class ContentChanged(
        val content: String,
    ) : InquiryScreenEvent

    data object SubmitClicked : InquiryScreenEvent

    data object RetryListClicked : InquiryScreenEvent

    // 목록 끝에 닿았거나 이어 받기 실패 후 재시도를 눌렀을 때 다음 문의를 요청한다.
    data object LoadMore : InquiryScreenEvent
}
