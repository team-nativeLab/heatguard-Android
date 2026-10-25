package com.nativelap.heartguard.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.auth.usecase.LoginAppAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class AppAccountLoginViewModel
    @Inject
    constructor(
        private val loginAppAccountUseCase: LoginAppAccountUseCase,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow(AppAccountLoginUiState())

        val uiState: StateFlow<AppAccountLoginUiState> = mutableUiState.asStateFlow()

        fun login(
            email: String,
            password: String,
        ) {
            if (mutableUiState.value.isSubmitting) {
                return
            }
            mutableUiState.update { it.copy(isSubmitting = true, hasFailure = false) }
            viewModelScope.launch {
                when (loginAppAccountUseCase(email.trim(), password)) {
                    is ApiResult.Success -> {
                        mutableUiState.update { it.copy(isSubmitting = false, isComplete = true) }
                    }

                    is ApiResult.Failure -> {
                        mutableUiState.update { it.copy(isSubmitting = false, hasFailure = true) }
                    }
                }
            }
        }
    }

internal data class AppAccountLoginUiState(
    val isSubmitting: Boolean = false,
    val hasFailure: Boolean = false,
    val isComplete: Boolean = false,
)
