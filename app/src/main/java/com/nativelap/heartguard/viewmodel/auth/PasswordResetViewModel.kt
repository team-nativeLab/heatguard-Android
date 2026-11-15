package com.nativelap.heartguard.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetError
import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetResult
import com.nativelap.heartguard.domain.auth.passwordreset.usecase.RequestPasswordResetEmailCodeUseCase
import com.nativelap.heartguard.domain.auth.passwordreset.usecase.ResetPasswordUseCase
import com.nativelap.heartguard.domain.auth.passwordreset.usecase.VerifyPasswordResetEmailCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Locale
import javax.inject.Inject

data class PasswordResetUiState(
    val step: Int = 0,
    val email: String = "",
    val code: String = "",
    val isLoading: Boolean = false,
    val failure: PasswordResetError? = null,
    val resendSeconds: Int = 0,
    val codeRemainingSeconds: Int = 0,
    val resetTokenRemainingSeconds: Int = 0,
    val completed: Boolean = false,
) {
    override fun toString(): String =
        "PasswordResetUiState(step=$step, email=[REDACTED], code=[REDACTED], " +
            "isLoading=$isLoading, failure=$failure, resendSeconds=$resendSeconds, " +
            "codeRemainingSeconds=$codeRemainingSeconds, " +
            "resetTokenRemainingSeconds=$resetTokenRemainingSeconds, completed=$completed)"
}

@HiltViewModel
class PasswordResetViewModel
    @Inject
    constructor(
        private val requestCode: RequestPasswordResetEmailCodeUseCase,
        private val verifyCode: VerifyPasswordResetEmailCodeUseCase,
        private val resetPassword: ResetPasswordUseCase,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(PasswordResetUiState())
        val uiState = mutableState.asStateFlow()
        private var resetToken: String? = null
        private var requestJob: Job? = null
        private var countdownJob: Job? = null
        private var operationId = 0L
        private var codeExpiresAt: Instant? = null
        private var tokenExpiresAt: Instant? = null
        private var resendAvailableAt: Instant? = null

        fun setEmail(email: String) {
            mutableState.update { it.copy(email = email, failure = null) }
        }

        fun setCode(code: String) {
            mutableState.update { it.copy(code = code.filter(Char::isDigit).take(6), failure = null) }
        }

        fun requestEmailCode() {
            val state = mutableState.value
            val email = state.email.trim().lowercase(Locale.ROOT)
            if (state.isLoading || state.resendSeconds > 0) return
            clearResetToken()
            mutableState.update { it.copy(isLoading = true, failure = null, code = "") }
            val op = beginRequest()
            requestJob =
                viewModelScope.launch {
                    try {
                        when (val result = requestCode(email)) {
                            is PasswordResetResult.Success -> {
                                if (op != operationId) return@launch
                                val now = Instant.now()
                                val expiration = result.value.expiresAt.toInstantOrNull()
                                if (expiration == null || !expiration.isAfter(now)) {
                                    mutableState.update {
                                        it.copy(isLoading = false, failure = PasswordResetError.Generic)
                                    }
                                    return@launch
                                }
                                codeExpiresAt = expiration
                                resendAvailableAt = now.plusSeconds(RESEND_SECONDS)
                                mutableState.update {
                                    it.copy(
                                        step = CODE_STEP,
                                        email = email,
                                        isLoading = false,
                                        codeRemainingSeconds = secondsUntil(expiration),
                                        resendSeconds = secondsUntil(resendAvailableAt),
                                    )
                                }
                                startCountdown()
                            }

                            is PasswordResetResult.Failure -> {
                                if (op == operationId) {
                                    mutableState.update { it.copy(isLoading = false, failure = result.error) }
                                }
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        if (op == operationId) {
                            mutableState.update {
                                it.copy(isLoading = false, failure = PasswordResetError.Generic)
                            }
                        }
                    }
                }
        }

        fun verifyEmailCode() {
            val state = mutableState.value
            if (state.isLoading || state.codeRemainingSeconds <= 0) return
            mutableState.update { it.copy(isLoading = true, failure = null) }
            val op = beginRequest()
            requestJob =
                viewModelScope.launch {
                    try {
                        when (val result = verifyCode(state.email, state.code)) {
                            is PasswordResetResult.Success -> {
                                if (op != operationId) return@launch
                                val expiration = result.value.expiresAt.toInstantOrNull()
                                if (expiration == null || !expiration.isAfter(Instant.now())) {
                                    mutableState.update {
                                        it.copy(
                                            isLoading = false,
                                            failure = PasswordResetError.InvalidToken,
                                        )
                                    }
                                    return@launch
                                }
                                resetToken = result.value.resetToken
                                tokenExpiresAt = expiration
                                mutableState.update {
                                    it.copy(
                                        step = PASSWORD_STEP,
                                        isLoading = false,
                                        resetTokenRemainingSeconds = secondsUntil(expiration),
                                    )
                                }
                                startCountdown()
                            }

                            is PasswordResetResult.Failure -> {
                                if (op == operationId) {
                                    mutableState.update { it.copy(isLoading = false, failure = result.error) }
                                }
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        if (op == operationId) {
                            mutableState.update {
                                it.copy(isLoading = false, failure = PasswordResetError.Generic)
                            }
                        }
                    }
                }
        }

        fun submitPassword(
            password: String,
            confirmation: String,
        ) {
            val state = mutableState.value
            val token = resetToken
            val expiration = tokenExpiresAt
            if (state.isLoading) return
            if (token == null || expiration == null || !expiration.isAfter(Instant.now())) {
                clearResetToken()
                mutableState.update {
                    it.copy(
                        step = CODE_STEP,
                        resetTokenRemainingSeconds = 0,
                        failure = PasswordResetError.InvalidToken,
                    )
                }
                return
            }
            mutableState.update { it.copy(isLoading = true, failure = null) }
            val op = beginRequest()
            requestJob =
                viewModelScope.launch {
                    try {
                        when (val result = resetPassword(state.email, token, password, confirmation)) {
                            is PasswordResetResult.Success -> {
                                if (op == operationId) {
                                    clearResetToken()
                                    mutableState.update { it.copy(isLoading = false, completed = true) }
                                }
                            }

                            is PasswordResetResult.Failure -> {
                                if (op == operationId) {
                                    if (result.error == PasswordResetError.InvalidToken) {
                                        clearResetToken()
                                        mutableState.update {
                                            it.copy(step = CODE_STEP, resetTokenRemainingSeconds = 0)
                                        }
                                    }
                                    mutableState.update { it.copy(isLoading = false, failure = result.error) }
                                }
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        if (op == operationId) {
                            mutableState.update {
                                it.copy(isLoading = false, failure = PasswordResetError.Generic)
                            }
                        }
                    }
                }
        }

        fun goBack() {
            val state = mutableState.value
            if (state.isLoading) return
            if (state.step >= PASSWORD_STEP) clearResetToken()
            mutableState.update {
                it.copy(
                    step = (it.step - 1).coerceAtLeast(EMAIL_STEP),
                    failure = null,
                    code = "",
                    resetTokenRemainingSeconds = 0,
                )
            }
        }

        fun cancelAndClear() {
            operationId++
            requestJob?.cancel()
            requestJob = null
            countdownJob?.cancel()
            countdownJob = null
            clearResetToken()
            codeExpiresAt = null
            resendAvailableAt = null
            mutableState.update { it.copy(isLoading = false, completed = false) }
        }

        private fun beginRequest(): Long {
            requestJob?.cancel()
            operationId++
            return operationId
        }

        private fun clearResetToken() {
            resetToken = null
            tokenExpiresAt = null
        }

        private fun startCountdown() {
            countdownJob?.cancel()
            countdownJob =
                viewModelScope.launch {
                    while (true) {
                        delay(1_000)
                        val now = Instant.now()
                        val codeSeconds = secondsUntil(codeExpiresAt, now)
                        val tokenSeconds = secondsUntil(tokenExpiresAt, now)
                        val resendSeconds = secondsUntil(resendAvailableAt, now)
                        if (tokenSeconds == 0 && resetToken != null) clearResetToken()
                        mutableState.update { current ->
                            if (current.step == PASSWORD_STEP && tokenSeconds == 0) {
                                current.copy(
                                    step = CODE_STEP,
                                    failure = PasswordResetError.InvalidToken,
                                    resetTokenRemainingSeconds = 0,
                                    codeRemainingSeconds = codeSeconds,
                                    resendSeconds = resendSeconds,
                                )
                            } else {
                                current.copy(
                                    codeRemainingSeconds = codeSeconds,
                                    resetTokenRemainingSeconds = tokenSeconds,
                                    resendSeconds = resendSeconds,
                                )
                            }
                        }
                        if (codeSeconds == 0 && tokenSeconds == 0 && resendSeconds == 0) {
                            break
                        }
                    }
                }
        }

        override fun onCleared() {
            cancelAndClear()
            super.onCleared()
        }

        private companion object {
            const val EMAIL_STEP = 0
            const val CODE_STEP = 1
            const val PASSWORD_STEP = 2
            const val RESEND_SECONDS = 60L
        }
    }

private fun String.toInstantOrNull(): Instant? = runCatching { Instant.parse(this) }.getOrNull()

private fun secondsUntil(
    deadline: Instant?,
    now: Instant = Instant.now(),
): Int = deadline?.let { (it.epochSecond - now.epochSecond).coerceAtLeast(0).toInt() } ?: 0
