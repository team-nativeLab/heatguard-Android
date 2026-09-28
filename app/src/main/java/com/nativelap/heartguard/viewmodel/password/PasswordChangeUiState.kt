package com.nativelap.heartguard.viewmodel.password

import androidx.compose.runtime.Immutable
import com.nativelap.heartguard.domain.profile.usecase.ChangeWorkerPasswordUseCase

/** 비밀번호 변경 화면 상태다(Figma 26~28). 입력값·보기 토글·요청 진행과 서버 오류를 가진다. */
@Immutable
data class PasswordChangeUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isCurrentPasswordVisible: Boolean = false,
    val isNewPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitError: PasswordChangeError? = null,
) {
    // 새 비밀번호가 "영문, 숫자를 포함해 8자 이상" 규칙을 지키는지다. 입력 전에는 false다.
    val isNewPasswordValid: Boolean
        get() = ChangeWorkerPasswordUseCase.isValidNewPassword(newPassword)

    // 확인 입력이 있고 새 비밀번호와 다를 때만 불일치로 본다(입력 전에는 오류를 보이지 않는다).
    val isConfirmMismatched: Boolean
        get() = confirmPassword.isNotEmpty() && confirmPassword != newPassword

    val isConfirmMatched: Boolean
        get() = confirmPassword.isNotEmpty() && confirmPassword == newPassword

    val canSubmit: Boolean
        get() = !isSubmitting &&
            currentPassword.isNotEmpty() &&
            isNewPasswordValid &&
            isConfirmMatched
}

/** 서버 요청 실패 종류다. 화면은 이 값으로 안내 문구를 고른다. */
enum class PasswordChangeError {
    INVALID_CURRENT_PASSWORD,
    INVALID_NEW_PASSWORD,
    FAILURE,
}
