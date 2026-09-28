package com.nativelap.heartguard.viewmodel.inquiry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.inquiry.usecase.SubmitInquiryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class InquiryViewModel @Inject constructor(
    private val submitInquiryUseCase: SubmitInquiryUseCase,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(InquiryUiState())
    val uiState: StateFlow<InquiryUiState> = mutableUiState.asStateFlow()

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
        viewModelScope.launch {
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
