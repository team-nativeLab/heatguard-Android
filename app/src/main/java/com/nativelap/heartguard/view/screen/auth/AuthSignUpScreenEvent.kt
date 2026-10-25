package com.nativelap.heartguard.view.screen.auth

/** 회원가입 화면에서 발생한 입력과 이동 의도를 나타낸다. */
internal sealed interface AuthSignUpScreenEvent {
    data class EmailChanged(
        val email: String,
    ) : AuthSignUpScreenEvent

    data class VerificationCodeChanged(
        val verificationCode: String,
    ) : AuthSignUpScreenEvent

    data class PasswordChanged(
        val password: String,
    ) : AuthSignUpScreenEvent

    data class PasswordConfirmationChanged(
        val passwordConfirmation: String,
    ) : AuthSignUpScreenEvent

    data object SignUpClicked : AuthSignUpScreenEvent

    data object RequestEmailCodeClicked : AuthSignUpScreenEvent

    data object LoginClicked : AuthSignUpScreenEvent
}
