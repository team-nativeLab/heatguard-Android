package com.nativelap.heartguard.viewmodel.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.clearStateWhenSessionEnds
import com.nativelap.heartguard.domain.profile.model.PasswordChangeResult
import com.nativelap.heartguard.domain.profile.usecase.ChangeWorkerPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** 비밀번호 변경 화면의 입력 검증과 변경 요청을 담당한다. 비밀번호가 메모리에 남지 않도록 화면 진입·세션 종료·성공 시 입력을 비운다. */
@HiltViewModel
class PasswordChangeViewModel @Inject constructor(
    private val changeWorkerPasswordUseCase: ChangeWorkerPasswordUseCase,
    sessionManager: SessionManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PasswordChangeUiState())
    val uiState: StateFlow<PasswordChangeUiState> = _uiState.asStateFlow()

    private val effectChannel = Channel<PasswordChangeEffect>(Channel.BUFFERED)
    val effects: Flow<PasswordChangeEffect> = effectChannel.receiveAsFlow()

    private var changePasswordJob: Job? = null

    init {
        clearStateWhenSessionEnds(sessionManager) {
            clearInput()
        }
    }

    /** 화면에 들어올 때 이전 입력을 비운다. Activity 수명 ViewModel이라 재진입 시 비밀번호가 남아 있으면 안 된다. */
    fun openScreen() {
        clearInput()
    }

    fun updateCurrentPassword(password: String) {
        _uiState.value = _uiState.value.copy(
            currentPassword = password,
            submitError = null,
        )
    }

    fun updateNewPassword(password: String) {
        _uiState.value = _uiState.value.copy(
            newPassword = password,
            submitError = null,
        )
    }

    fun updateConfirmPassword(password: String) {
        _uiState.value = _uiState.value.copy(
            confirmPassword = password,
            submitError = null,
        )
    }

    fun toggleCurrentPasswordVisibility() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            isCurrentPasswordVisible = !currentState.isCurrentPasswordVisible,
        )
    }

    fun toggleNewPasswordVisibility() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            isNewPasswordVisible = !currentState.isNewPasswordVisible,
        )
    }

    fun toggleConfirmPasswordVisibility() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            isConfirmPasswordVisible = !currentState.isConfirmPasswordVisible,
        )
    }

    /** 입력이 규칙을 만족할 때만 서버에 변경을 요청한다. 성공하면 입력을 비우고 [PasswordChangeEffect.Changed]를 한 번 보낸다.
     * 현재 비밀번호가 틀려도 세션은 유지되고 오류만 표시한다. */
    fun submit() {
        val currentState = _uiState.value
        if (!currentState.canSubmit) {
            return
        }

        _uiState.value = currentState.copy(
            isSubmitting = true,
            submitError = null,
        )
        changePasswordJob = viewModelScope.launch {
            val changeResult = changeWorkerPasswordUseCase(
                currentPassword = currentState.currentPassword,
                newPassword = currentState.newPassword,
            )
            when (changeResult) {
                PasswordChangeResult.Success -> {
                    _uiState.value = PasswordChangeUiState()
                    effectChannel.send(PasswordChangeEffect.Changed)
                }

                PasswordChangeResult.InvalidCurrentPassword -> {
                    showSubmitError(PasswordChangeError.INVALID_CURRENT_PASSWORD)
                }

                PasswordChangeResult.InvalidNewPassword -> {
                    showSubmitError(PasswordChangeError.INVALID_NEW_PASSWORD)
                }

                PasswordChangeResult.Failure -> {
                    showSubmitError(PasswordChangeError.FAILURE)
                }
            }
        }
    }

    private fun showSubmitError(passwordChangeError: PasswordChangeError) {
        _uiState.value = _uiState.value.copy(
            isSubmitting = false,
            submitError = passwordChangeError,
        )
    }

    private fun clearInput() {
        changePasswordJob?.cancel()
        changePasswordJob = null
        _uiState.value = PasswordChangeUiState()
    }
}
