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

    fun onTitleChanged(title: String) {
        mutableUiState.value = mutableUiState.value.copy(
            title = title,
            submissionError = null,
            submittedInquiry = null,
        )
    }

    fun onContentChanged(content: String) {
        mutableUiState.value = mutableUiState.value.copy(
            content = content,
            submissionError = null,
            submittedInquiry = null,
        )
    }

    fun submit() {
        val currentState = mutableUiState.value
        if (!currentState.canSubmit) {
            return
        }

        mutableUiState.value = currentState.copy(isSubmitting = true, submissionError = null)
        viewModelScope.launch {
            when (val result = submitInquiryUseCase(currentState.title, currentState.content)) {
                is ApiResult.Success -> {
                    mutableUiState.value = InquiryUiState(submittedInquiry = result.value)
                }

                is ApiResult.Failure -> {
                    mutableUiState.value = mutableUiState.value.copy(
                        isSubmitting = false,
                        submissionError = result.error,
                    )
                }
            }
        }
    }

    fun clearSubmissionMessage() {
        mutableUiState.value = mutableUiState.value.copy(submittedInquiry = null)
    }
}
