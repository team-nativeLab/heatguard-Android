package com.nativelap.heartguard.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.auth.usecase.RegisterAppAccountUseCase
import com.nativelap.heartguard.domain.auth.usecase.RequestAppEmailCodeUseCase
import com.nativelap.heartguard.domain.auth.usecase.VerifyAppEmailCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
internal class AppAccountSignUpViewModel
    @Inject
    constructor(
        private val requestAppEmailCodeUseCase: RequestAppEmailCodeUseCase,
        private val verifyAppEmailCodeUseCase: VerifyAppEmailCodeUseCase,
        private val registerAppAccountUseCase: RegisterAppAccountUseCase,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow(AppAccountSignUpUiState())
        private var verifiedEmail: String? = null
        private var verificationToken: String? = null
        private val emailCodeExpirationJobs = mutableMapOf<String, Job>()
        private val emailCodeCooldownJobs = mutableMapOf<String, Job>()

        val uiState: StateFlow<AppAccountSignUpUiState> = mutableUiState.asStateFlow()

        fun requestEmailCode(email: String) {
            val normalizedEmail = email.trim().lowercase(Locale.ROOT)
            if (normalizedEmail.isBlank() || mutableUiState.value.isRequestingEmailCode ||
                mutableUiState.value.isVerifyingEmailCode || mutableUiState.value.isSubmitting ||
                (mutableUiState.value.emailCodeCooldownSecondsByEmail[normalizedEmail] ?: 0) > 0
            ) {
                return
            }
            mutableUiState.update {
                it.copy(
                    isRequestingEmailCode = true,
                    verifiedEmail = null,
                    failure = null,
                    failureMessage = null,
                )
            }
            verifiedEmail = null
            verificationToken = null
            viewModelScope.launch {
                val result = requestAppEmailCodeUseCase(normalizedEmail)
                when (result) {
                    is ApiResult.Success -> {
                        mutableUiState.update {
                            it.copy(
                                isRequestingEmailCode = false,
                                isEmailCodeSent = true,
                                emailForRequestedCode = normalizedEmail,
                                emailCodeCooldownSecondsByEmail =
                                    it.emailCodeCooldownSecondsByEmail +
                                        (normalizedEmail to EMAIL_CODE_RESEND_COOLDOWN_SECONDS),
                                emailCodeRemainingSecondsByEmail =
                                    it.emailCodeRemainingSecondsByEmail +
                                        (normalizedEmail to remainingSeconds(result.value.expiresAt)),
                            )
                        }
                        startEmailCodeExpirationCountdown(normalizedEmail, result.value.expiresAt)
                        startEmailCodeCooldown(normalizedEmail)
                    }

                    is ApiResult.Failure -> {
                        mutableUiState.update {
                            it.copy(
                                isRequestingEmailCode = false,
                                failure = result.error.toEmailCodeFailure(),
                                failureMessage = result.error.serverMessage(),
                            )
                        }
                    }
                }
            }
        }

        private fun startEmailCodeCooldown(email: String) {
            emailCodeCooldownJobs[email]?.cancel()
            emailCodeCooldownJobs[email] =
                viewModelScope.launch {
                    for (remainingSeconds in EMAIL_CODE_RESEND_COOLDOWN_SECONDS downTo 1) {
                        delay(ONE_SECOND_MILLIS)
                        mutableUiState.update {
                            it.copy(
                                emailCodeCooldownSecondsByEmail =
                                    it.emailCodeCooldownSecondsByEmail +
                                        (email to (remainingSeconds - 1)),
                            )
                        }
                    }
                    mutableUiState.update {
                        it.copy(emailCodeCooldownSecondsByEmail = it.emailCodeCooldownSecondsByEmail - email)
                    }
                    emailCodeCooldownJobs.remove(email)
                }
        }

        private fun startEmailCodeExpirationCountdown(
            email: String,
            expiresAt: String,
        ) {
            emailCodeExpirationJobs[email]?.cancel()
            emailCodeExpirationJobs[email] =
                viewModelScope.launch {
                    val expirationEpochSeconds = parseExpirationEpochSeconds(expiresAt)
                    while (true) {
                        val remainingSeconds =
                            (expirationEpochSeconds - Instant.now().epochSecond)
                                .coerceAtLeast(0)
                                .toInt()
                        mutableUiState.update {
                            it.copy(
                                emailCodeRemainingSecondsByEmail =
                                    if (remainingSeconds == 0) {
                                        it.emailCodeRemainingSecondsByEmail - email
                                    } else {
                                        it.emailCodeRemainingSecondsByEmail + (email to remainingSeconds)
                                    },
                            )
                        }
                        if (remainingSeconds == 0) {
                            break
                        }
                        delay(ONE_SECOND_MILLIS)
                    }
                    emailCodeExpirationJobs.remove(email)
                }
        }

        private fun remainingSeconds(expiresAt: String): Int =
            (parseExpirationEpochSeconds(expiresAt) - Instant.now().epochSecond)
                .coerceAtLeast(0)
                .toInt()

        private fun parseExpirationEpochSeconds(expiresAt: String): Long =
            runCatching { OffsetDateTime.parse(expiresAt).toEpochSecond() }
                .getOrElse { Instant.now().plusSeconds(EMAIL_CODE_VALIDITY_SECONDS).epochSecond }

        fun verifyEmailCode(
            email: String,
            verificationCode: String,
        ) {
            val normalizedEmail = email.trim().lowercase(Locale.ROOT)
            val normalizedCode = verificationCode.filter { digit -> digit in '0'..'9' }
            if (mutableUiState.value.isVerifyingEmailCode || mutableUiState.value.isSubmitting) {
                return
            }
            if (!mutableUiState.value.isEmailCodeSent ||
                mutableUiState.value.emailForRequestedCode != normalizedEmail
            ) {
                mutableUiState.update {
                    it.copy(
                        failure = AppAccountSignUpFailure.EMAIL_CODE_REQUIRED,
                        failureMessage = null,
                    )
                }
                return
            }
            if (normalizedCode.length != VERIFICATION_CODE_LENGTH) {
                mutableUiState.update {
                    it.copy(
                        failure = AppAccountSignUpFailure.INVALID_FORM,
                        failureMessage = null,
                    )
                }
                return
            }

            mutableUiState.update {
                it.copy(
                    isVerifyingEmailCode = true,
                    verifiedEmail = null,
                    failure = null,
                    failureMessage = null,
                )
            }
            verifiedEmail = null
            verificationToken = null
            viewModelScope.launch {
                when (val result = verifyAppEmailCodeUseCase(normalizedEmail, normalizedCode)) {
                    is ApiResult.Success -> {
                        verifiedEmail = normalizedEmail
                        verificationToken = result.value.verificationToken
                        mutableUiState.update {
                            it.copy(
                                isVerifyingEmailCode = false,
                                verifiedEmail = normalizedEmail,
                            )
                        }
                    }

                    is ApiResult.Failure -> {
                        mutableUiState.update {
                            it.copy(
                                isVerifyingEmailCode = false,
                                failure = result.error.toVerificationFailure(),
                                failureMessage = result.error.serverMessage(),
                            )
                        }
                    }
                }
            }
        }

        fun clearEmailCodeVerification() {
            verifiedEmail = null
            verificationToken = null
            mutableUiState.update {
                it.copy(
                    verifiedEmail = null,
                    failure = null,
                    failureMessage = null,
                )
            }
        }

        fun signUp(
            email: String,
            password: String,
            confirmPassword: String,
        ) {
            val normalizedEmail = email.trim().lowercase(Locale.ROOT)
            if (mutableUiState.value.isSubmitting || mutableUiState.value.isRequestingEmailCode ||
                mutableUiState.value.isVerifyingEmailCode
            ) {
                return
            }
            if (!mutableUiState.value.isEmailCodeSent ||
                mutableUiState.value.emailForRequestedCode != normalizedEmail
            ) {
                mutableUiState.update {
                    it.copy(
                        failure = AppAccountSignUpFailure.EMAIL_CODE_REQUIRED,
                        failureMessage = null,
                    )
                }
                return
            }
            val verifiedToken = verificationToken
            if (verifiedEmail != normalizedEmail || verifiedToken.isNullOrBlank()) {
                mutableUiState.update {
                    it.copy(
                        failure = AppAccountSignUpFailure.EMAIL_CODE_REQUIRED,
                        failureMessage = null,
                    )
                }
                return
            }
            if (!password.isValidSignUpPassword() ||
                password != confirmPassword
            ) {
                mutableUiState.update {
                    it.copy(
                        failure = AppAccountSignUpFailure.INVALID_FORM,
                        failureMessage = null,
                    )
                }
                return
            }

            mutableUiState.update {
                it.copy(
                    isSubmitting = true,
                    failure = null,
                    failureMessage = null,
                )
            }
            viewModelScope.launch {
                val registrationResult =
                    registerAppAccountUseCase(
                        email = normalizedEmail,
                        password = password,
                        confirmPassword = confirmPassword,
                        verificationToken = verifiedToken,
                    )
                when (registrationResult) {
                    is ApiResult.Failure -> {
                        if (registrationResult.error.isInvalidVerificationCode()) {
                            verifiedEmail = null
                            verificationToken = null
                        }
                        mutableUiState.update {
                            it.copy(
                                isSubmitting = false,
                                verifiedEmail =
                                    if (registrationResult.error.isInvalidVerificationCode()) {
                                        null
                                    } else {
                                        it.verifiedEmail
                                    },
                                failure = registrationResult.error.toRegistrationFailure(),
                                failureMessage = registrationResult.error.serverMessage(),
                            )
                        }
                    }

                    is ApiResult.Success -> {
                        verifiedEmail = null
                        verificationToken = null
                        emailCodeExpirationJobs.values.forEach(Job::cancel)
                        emailCodeExpirationJobs.clear()
                        mutableUiState.update {
                            it.copy(
                                isSubmitting = false,
                                verifiedEmail = null,
                                isComplete = true,
                                emailCodeRemainingSecondsByEmail = emptyMap(),
                            )
                        }
                    }
                }
            }
        }

        private fun ApiError.toEmailCodeFailure(): AppAccountSignUpFailure =
            when (this) {
                is ApiError.Http -> {
                    when {
                        statusCode == HTTP_TOO_MANY_REQUESTS -> {
                            AppAccountSignUpFailure.RATE_LIMITED
                        }

                        else -> {
                            AppAccountSignUpFailure.EMAIL_CODE_REQUEST
                        }
                    }
                }

                ApiError.Network -> {
                    AppAccountSignUpFailure.NETWORK
                }

                ApiError.SessionChanged,
                ApiError.SiteNotSelected,
                ApiError.Serialization,
                ApiError.Unknown,
                ApiError.LocalStorage,
                is ApiError.ServerRejected,
                -> {
                    AppAccountSignUpFailure.EMAIL_CODE_REQUEST
                }
            }

        private fun ApiError.serverMessage(): String? =
            when (this) {
                is ApiError.Http -> errorMessage

                is ApiError.ServerRejected -> errorMessage

                ApiError.Network,
                ApiError.SessionChanged,
                ApiError.SiteNotSelected,
                ApiError.Serialization,
                ApiError.LocalStorage,
                ApiError.Unknown,
                -> null
            }?.takeIf { errorMessage -> errorMessage.isNotBlank() }

        private fun ApiError.toRegistrationFailure(): AppAccountSignUpFailure =
            when (this) {
                is ApiError.Http -> {
                    when {
                        errorCode == INVALID_VERIFICATION_CODE -> {
                            AppAccountSignUpFailure.INVALID_VERIFICATION_CODE
                        }

                        statusCode == HTTP_CONFLICT && errorCode == EMAIL_ALREADY_EXISTS_CODE -> {
                            AppAccountSignUpFailure.EMAIL_ALREADY_EXISTS
                        }

                        statusCode == HTTP_CONFLICT && errorCode == SILO_NOT_BOOTSTRAPPED -> {
                            AppAccountSignUpFailure.ACCOUNT_NOT_READY
                        }

                        statusCode == HTTP_TOO_MANY_REQUESTS -> {
                            AppAccountSignUpFailure.RATE_LIMITED
                        }

                        else -> {
                            AppAccountSignUpFailure.REGISTRATION
                        }
                    }
                }

                ApiError.Network -> {
                    AppAccountSignUpFailure.NETWORK
                }

                ApiError.SessionChanged,
                ApiError.SiteNotSelected,
                ApiError.Serialization,
                ApiError.Unknown,
                ApiError.LocalStorage,
                is ApiError.ServerRejected,
                -> {
                    AppAccountSignUpFailure.REGISTRATION
                }
            }

        private fun ApiError.toVerificationFailure(): AppAccountSignUpFailure =
            when (this) {
                is ApiError.Http -> {
                    when {
                        errorCode == INVALID_VERIFICATION_CODE -> {
                            AppAccountSignUpFailure.INVALID_VERIFICATION_CODE
                        }

                        statusCode == HTTP_TOO_MANY_REQUESTS -> {
                            AppAccountSignUpFailure.RATE_LIMITED
                        }

                        else -> {
                            AppAccountSignUpFailure.EMAIL_CODE_VERIFICATION
                        }
                    }
                }

                ApiError.Network -> {
                    AppAccountSignUpFailure.NETWORK
                }

                ApiError.SessionChanged,
                ApiError.SiteNotSelected,
                ApiError.Serialization,
                ApiError.Unknown,
                ApiError.LocalStorage,
                is ApiError.ServerRejected,
                -> {
                    AppAccountSignUpFailure.EMAIL_CODE_VERIFICATION
                }
            }

        private fun ApiError.isInvalidVerificationCode(): Boolean =
            this is ApiError.Http && errorCode == INVALID_VERIFICATION_CODE

        private fun String.isValidSignUpPassword(): Boolean =
            length >= MIN_PASSWORD_LENGTH &&
                any { character -> character in 'A'..'Z' || character in 'a'..'z' } &&
                any { character -> character in '0'..'9' }

        private companion object {
            const val VERIFICATION_CODE_LENGTH = 6
            const val EMAIL_CODE_RESEND_COOLDOWN_SECONDS = 60
            const val EMAIL_CODE_VALIDITY_SECONDS = 600L
            const val ONE_SECOND_MILLIS = 1_000L
            const val MIN_PASSWORD_LENGTH = 8
            const val HTTP_CONFLICT = 409
            const val HTTP_TOO_MANY_REQUESTS = 429
            const val EMAIL_ALREADY_EXISTS_CODE = "EMAIL_ALREADY_EXISTS"
            const val INVALID_VERIFICATION_CODE = "INVALID_VERIFICATION_CODE"
            const val SILO_NOT_BOOTSTRAPPED = "SILO_NOT_BOOTSTRAPPED"
        }
    }

internal data class AppAccountSignUpUiState(
    val isRequestingEmailCode: Boolean = false,
    val isEmailCodeSent: Boolean = false,
    val isVerifyingEmailCode: Boolean = false,
    val emailCodeCooldownSecondsByEmail: Map<String, Int> = emptyMap(),
    val emailCodeRemainingSecondsByEmail: Map<String, Int> = emptyMap(),
    val emailForRequestedCode: String? = null,
    val verifiedEmail: String? = null,
    val isSubmitting: Boolean = false,
    val isComplete: Boolean = false,
    val failure: AppAccountSignUpFailure? = null,
    val failureMessage: String? = null,
)

internal enum class AppAccountSignUpFailure {
    EMAIL_CODE_REQUIRED,
    EMAIL_CODE_REQUEST,
    EMAIL_CODE_VERIFICATION,
    EMAIL_ALREADY_EXISTS,
    RATE_LIMITED,
    NETWORK,
    INVALID_FORM,
    INVALID_VERIFICATION_CODE,
    ACCOUNT_NOT_READY,
    REGISTRATION,
}
