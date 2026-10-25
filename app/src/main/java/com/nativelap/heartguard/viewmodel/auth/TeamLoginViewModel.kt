package com.nativelap.heartguard.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.domain.auth.model.TeamLoginResult
import com.nativelap.heartguard.domain.auth.usecase.TeamLoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeamLoginViewModel
    @Inject
    constructor(
        private val teamLoginUseCase: TeamLoginUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(TeamLoginUiState())
        val uiState: StateFlow<TeamLoginUiState> = _uiState.asStateFlow()

        /** 로그인 화면 제출 시 중복 요청을 막고 서버 결과를 화면 상태로 변환한다. */
        fun login(
            email: String,
            password: String,
        ) {
            if (_uiState.value.isSubmitting) return

            _uiState.value = TeamLoginUiState(isSubmitting = true)
            viewModelScope.launch {
                val result = teamLoginUseCase(email = email, password = password)
                _uiState.value =
                    TeamLoginUiState(
                        isSubmitting = false,
                        failure =
                            when (result) {
                                TeamLoginResult.Success -> null
                                TeamLoginResult.InvalidCredentials -> TeamLoginFailure.INVALID_CREDENTIALS
                                TeamLoginResult.AccountDisabled -> TeamLoginFailure.ACCOUNT_DISABLED
                                TeamLoginResult.RateLimited -> TeamLoginFailure.RATE_LIMITED
                                TeamLoginResult.Failure -> TeamLoginFailure.GENERIC
                            },
                    )
            }
        }
    }

data class TeamLoginUiState(
    val isSubmitting: Boolean = false,
    val failure: TeamLoginFailure? = null,
)

enum class TeamLoginFailure {
    INVALID_CREDENTIALS,
    ACCOUNT_DISABLED,
    RATE_LIMITED,
    GENERIC,
}
