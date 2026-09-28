package com.nativelap.heartguard.viewmodel.inquiry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.clearStateWhenSessionEnds
import com.nativelap.heartguard.domain.inquiry.usecase.SubmitInquiryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class InquiryViewModel @Inject constructor(
    private val submitInquiryUseCase: SubmitInquiryUseCase,
    sessionManager: SessionManager,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(InquiryUiState())
    val uiState: StateFlow<InquiryUiState> = mutableUiState.asStateFlow()

    private var submitInquiryJob: Job? = null

    init {
        // Activity 수명 ViewModel이라 로그아웃 뒤 다른 작업자가 로그인해도 이전 입력·접수 결과가 남지 않게 한다.
        clearStateWhenSessionEnds(sessionManager) {
            submitInquiryJob?.cancel()
            submitInquiryJob = null
            mutableUiState.value = InquiryUiState()
        }
    }

    /** 제목 입력을 현재 화면 상태에 반영하고 이전 요청 결과를 지운다. */
    fun updateTitle(title: String) {
        mutableUiState.value = mutableUiState.value.copy(
            title = title,
            submission = null,
            hasError = false,
        )
    }

    /** 문의 내용 입력을 현재 화면 상태에 반영하고 이전 요청 결과를 지운다. */
    fun updateContent(content: String) {
        mutableUiState.value = mutableUiState.value.copy(
            content = content,
            submission = null,
            hasError = false,
        )
    }

    /** 제목과 내용을 서버에 등록하고 접수 결과 또는 재시도 가능한 오류 상태를 반영한다. */
    fun submitInquiry() {
        val currentState = mutableUiState.value
        if (currentState.isSubmitting || currentState.title.isBlank() || currentState.content.isBlank()) {
            return
        }

        mutableUiState.value = currentState.copy(
            isSubmitting = true,
            submission = null,
            hasError = false,
        )
        submitInquiryJob = viewModelScope.launch {
            when (
                val result = submitInquiryUseCase(
                    title = currentState.title,
                    content = currentState.content,
                )
            ) {
                is ApiResult.Success -> {
                    mutableUiState.value = mutableUiState.value.copy(
                        isSubmitting = false,
                        submission = result.value,
                    )
                }

                is ApiResult.Failure -> {
                    mutableUiState.value = mutableUiState.value.copy(
                        isSubmitting = false,
                        hasError = true,
                    )
                }
            }
        }
    }
}
