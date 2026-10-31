package com.nativelap.heartguard.viewmodel.inquiry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InquiryUiStateTest {
    @Test
    fun `submission is blocked while a request is in progress`() {
        val uiState = InquiryUiState(title = "제목", content = "내용", isSubmitting = true)

        assertFalse(uiState.canSubmitInquiry())
    }

    @Test
    fun `submission requires a nonblank title and content`() {
        assertFalse(InquiryUiState(title = "", content = "내용").canSubmitInquiry())
        assertFalse(InquiryUiState(title = "제목", content = "  ").canSubmitInquiry())
        assertTrue(InquiryUiState(title = "제목", content = "내용").canSubmitInquiry())
    }

    @Test
    fun `submission failure preserves entered content and allows retry`() {
        val failedState =
            InquiryUiState(
                title = "제목",
                content = "내용",
                isSubmitting = true,
            ).afterSubmissionFailure()

        assertEquals("제목", failedState.title)
        assertEquals("내용", failedState.content)
        assertTrue(failedState.hasError)
        assertTrue(failedState.canSubmitInquiry())
    }
}
