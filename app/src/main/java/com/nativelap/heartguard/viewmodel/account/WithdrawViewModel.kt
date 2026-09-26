package com.nativelap.heartguard.viewmodel.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.domain.account.model.WithdrawReason
import com.nativelap.heartguard.domain.account.usecase.WithdrawAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 회원탈퇴 안내 → 최종 확인 → 완료 흐름이 공유하는 ViewModel이다.
 * HeartGuardMainNavDisplay에서 Activity 스코프로 만들어 여러 목적지가 같은 인스턴스를 보므로,
 * 흐름을 벗어날 때마다 [reset]으로 입력값(특히 비밀번호)과 진행 중인 요청을 반드시 정리해야 한다. */
@HiltViewModel
class WithdrawViewModel @Inject constructor(
    private val withdrawAccountUseCase: WithdrawAccountUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WithdrawUiState())
    val uiState: StateFlow<WithdrawUiState> = _uiState.asStateFlow()

    // 비밀번호는 UiState data class 밖에 따로 두어 로그·toString에 섞이지 않게 한다.
    // 프로세스 종료 시 디스크에 남지 않도록 rememberSaveable·SavedStateHandle에도 저장하지 않는다.
    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private var withdrawJob: Job? = null
    private var isFinishing = false

    fun selectReason(reason: WithdrawReason?) {
        _uiState.update { it.copy(selectedReason = reason) }
    }

    /** 비밀번호 입력이 바뀔 때 호출한다. 직전 요청이 실패했다면 다시 입력하는 시점에 오류 안내를 지운다. */
    fun updatePassword(password: String) {
        _password.value = password
        _uiState.update { state ->
            state.copy(
                isPasswordEntered = password.isNotBlank(),
                submissionState = state.submissionState.clearedFailure(),
            )
        }
    }

    fun updateAgreement(isAgreed: Boolean) {
        _uiState.update { it.copy(isAgreed = isAgreed) }
    }

    /** 최종 확인 다이얼로그에서 "탈퇴하기"를 눌렀을 때 호출한다. 입력 조건을 만족하지 않거나 이미 요청 중이면 무시한다.
     * 결과는 [WithdrawUiState.submissionState]의 Succeeded/Failed로 알린다. */
    fun withdraw() {
        if (!_uiState.value.canSubmit) {
            return
        }

        _uiState.update { it.copy(submissionState = WithdrawSubmissionState.Submitting) }
        withdrawJob = viewModelScope.launch {
            val withdrawResult = withdrawAccountUseCase(
                password = _password.value,
                reason = _uiState.value.selectedReason,
            )
            val nextSubmissionState = when (withdrawResult) {
                is ApiResult.Success -> WithdrawSubmissionState.Succeeded
                is ApiResult.Failure -> WithdrawSubmissionState.Failed
            }
            _uiState.update { it.copy(submissionState = nextSubmissionState) }
        }
    }

    /** 탈퇴 완료 화면에서 확인(또는 뒤로가기)을 눌렀을 때 호출한다. 입력값을 정리한 뒤 세션을 종료해
     * HeartGuardNavHost가 로그인 화면으로 전환하게 한다. 연속 호출은 한 번만 처리한다. */
    fun finishWithdrawal() {
        if (isFinishing) {
            return
        }

        isFinishing = true
        reset()
        viewModelScope.launch {
            try {
                sessionManager.expireSession()
            } finally {
                isFinishing = false
            }
        }
    }

    /** 흐름을 벗어날 때 호출한다. 진행 중인 요청을 취소해, 늦게 도착한 결과가 다시 들어온 흐름에 반영되지 않게 한다. */
    fun reset() {
        withdrawJob?.cancel()
        withdrawJob = null
        _password.value = ""
        _uiState.value = WithdrawUiState()
    }

    private fun WithdrawSubmissionState.clearedFailure(): WithdrawSubmissionState {
        return if (this == WithdrawSubmissionState.Failed) {
            WithdrawSubmissionState.Idle
        } else {
            this
        }
    }
}
