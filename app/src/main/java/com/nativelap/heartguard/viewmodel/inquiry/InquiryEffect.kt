package com.nativelap.heartguard.viewmodel.inquiry

/** 문의 등록 성공 후 화면이 한 번만 반응해야 하는 효과다. */
sealed interface InquiryEffect {
    data object NavigateToList : InquiryEffect
}
