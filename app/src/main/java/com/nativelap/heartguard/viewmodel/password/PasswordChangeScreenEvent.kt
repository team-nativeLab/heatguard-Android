package com.nativelap.heartguard.viewmodel.password

/** 비밀번호 변경 화면에서 발생하는 사용자 의도를 Route에 전달한다. */
sealed interface PasswordChangeScreenEvent {
    data object BackClicked : PasswordChangeScreenEvent

    data class CurrentPasswordChanged(
        val password: String,
    ) : PasswordChangeScreenEvent

    data class NewPasswordChanged(
        val password: String,
    ) : PasswordChangeScreenEvent

    data class ConfirmPasswordChanged(
        val password: String,
    ) : PasswordChangeScreenEvent

    data object CurrentPasswordVisibilityClicked : PasswordChangeScreenEvent

    data object NewPasswordVisibilityClicked : PasswordChangeScreenEvent

    data object ConfirmPasswordVisibilityClicked : PasswordChangeScreenEvent

    data object SubmitClicked : PasswordChangeScreenEvent
}
