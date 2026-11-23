package com.nativelap.heartguard.domain.inquiry.model

/** 앱 재시작 후 미확정 문의 등록을 확인하는 데 필요한 최소 상태다. */
data class InquirySubmissionGuard(
    val baselineInquiryIds: Set<String>,
)

data class InquirySubmissionConfirmation(
    val submittedDraftIsUnchanged: Boolean,
)
