package com.nativelap.heartguard.view.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetError
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.account.WithdrawTopBar
import com.nativelap.heartguard.view.component.auth.AuthPrimaryButton
import com.nativelap.heartguard.view.component.auth.AuthTextField
import com.nativelap.heartguard.view.component.auth.AuthTextFieldStyle
import com.nativelap.heartguard.view.component.auth.AuthTitleBlock
import com.nativelap.heartguard.view.component.auth.AuthVerificationCodeInput
import com.nativelap.heartguard.view.component.password.PasswordInputField

@Composable
fun AuthPasswordResetScreen(
    step: Int,
    email: String,
    code: String,
    isLoading: Boolean,
    failure: PasswordResetError?,
    resendSeconds: Int,
    codeRemainingSeconds: Int,
    resetTokenRemainingSeconds: Int,
    onBackClick: () -> Unit,
    onEmailChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onRequestCode: () -> Unit,
    onVerifyCode: () -> Unit,
    onSubmitPassword: (String, String) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmationVisible by remember { mutableStateOf(false) }
    LaunchedEffect(step) {
        if (step < 2) {
            password = ""
            confirmation = ""
            passwordVisible = false
            confirmationVisible = false
        }
    }
    val errorMessage = failure?.let { error -> stringResource(error.toStringResource()) }
    Scaffold(containerColor = MaterialTheme.extraColors.authBackground) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            WithdrawTopBar(
                title = stringResource(R.string.auth_password_reset_title),
                backContentDescription = stringResource(R.string.common_back_description),
                onBackClick = if (isLoading) null else onBackClick,
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = HeartGuardSpacing.AuthHorizontal),
                verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.AuthFieldGroup),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    repeat(3) { index ->
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .background(
                                        color =
                                            if (index <= step) {
                                                MaterialTheme.extraColors.authPrimary
                                            } else {
                                                MaterialTheme.extraColors.authInputBackground
                                            },
                                        shape = RoundedCornerShape(HeartGuardSpacing.Tight),
                                    ),
                        )
                    }
                }
                Text(
                    text =
                        stringResource(
                            when (step) {
                                0 -> R.string.auth_password_reset_step_email
                                1 -> R.string.auth_password_reset_step_code
                                else -> R.string.auth_password_reset_step_password
                            },
                        ),
                    color = MaterialTheme.extraColors.authPrimary,
                    style = MaterialTheme.typography.labelLarge,
                )
                AuthTitleBlock(
                    title =
                        stringResource(
                            when (step) {
                                0 -> R.string.auth_password_reset_email_title
                                1 -> R.string.auth_password_reset_code_title
                                else -> R.string.auth_password_reset_password_title
                            },
                        ),
                    description =
                        stringResource(
                            when (step) {
                                0 -> R.string.auth_password_reset_email_step_description
                                1 -> R.string.auth_password_reset_code_step_description
                                else -> R.string.auth_password_reset_password_step_description
                            },
                        ),
                )
                when (step) {
                    0 -> {
                        AuthTextField(
                            label = stringResource(R.string.auth_password_reset_email_label),
                            text = email,
                            onTextChange = onEmailChange,
                            placeholder = stringResource(R.string.auth_password_reset_email_hint),
                            enabled = !isLoading,
                            style = AuthTextFieldStyle.SoftFilled,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        )
                        if (resendSeconds > 0) {
                            Text(
                                text = stringResource(R.string.auth_password_reset_resend_waiting, resendSeconds),
                                color = MaterialTheme.extraColors.authOnSurfaceVariant,
                            )
                        }
                    }

                    1 -> {
                        Text(text = email, style = MaterialTheme.typography.bodyMedium)
                        AuthVerificationCodeInput(
                            code = code,
                            onCodeChange = onCodeChange,
                            enabled = !isLoading,
                            errorDescription = errorMessage,
                        )
                        Text(
                            text =
                                if (codeRemainingSeconds >
                                    0
                                ) {
                                    "${codeRemainingSeconds / 60}:${(codeRemainingSeconds % 60).toString().padStart(
                                        2,
                                        '0',
                                    )}"
                                } else {
                                    stringResource(R.string.auth_password_reset_code_expired)
                                },
                            color = MaterialTheme.extraColors.authOnSurfaceVariant,
                        )
                        Text(
                            text =
                                if (resendSeconds >
                                    0
                                ) {
                                    stringResource(R.string.auth_password_reset_resend_waiting, resendSeconds)
                                } else {
                                    stringResource(R.string.auth_password_reset_resend)
                                },
                            color = MaterialTheme.extraColors.authPrimary,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }

                    else -> {
                        PasswordInputField(
                            label = stringResource(R.string.auth_password_reset_password_label),
                            password = password,
                            placeholder = stringResource(R.string.auth_password_reset_password_hint),
                            isPasswordVisible = passwordVisible,
                            onPasswordChange = { password = it },
                            onVisibilityClick = { passwordVisible = !passwordVisible },
                            enabled = !isLoading,
                        )
                        PasswordInputField(
                            label = stringResource(R.string.auth_password_reset_confirm_label),
                            password = confirmation,
                            placeholder = stringResource(R.string.auth_password_reset_confirm_hint),
                            isPasswordVisible = confirmationVisible,
                            onPasswordChange = { confirmation = it },
                            onVisibilityClick = {
                                confirmationVisible =
                                    !confirmationVisible
                            },
                            enabled = !isLoading,
                        )
                    }
                }
                if (errorMessage != null && step != 1) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                }
                if (step == 2) {
                    Text(
                        text =
                            stringResource(
                                R.string.auth_password_reset_token_remaining,
                                resetTokenRemainingSeconds / 60,
                                resetTokenRemainingSeconds % 60,
                            ),
                        color = MaterialTheme.extraColors.authOnSurfaceVariant,
                    )
                }
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HeartGuardSpacing.AuthHorizontal)
                        .padding(bottom = HeartGuardSpacing.AuthActionBottom),
                verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
            ) {
                if (step == 1 && resendSeconds == 0) {
                    androidx.compose.material3.TextButton(
                        onClick = onRequestCode,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.auth_password_reset_resend))
                    }
                }
                AuthPrimaryButton(
                    title =
                        stringResource(
                            when (step) {
                                0 -> R.string.auth_password_reset_send
                                1 -> R.string.password_reset_verify
                                else -> R.string.auth_password_reset_submit
                            },
                        ),
                    onClick = {
                        when (step) {
                            0 -> onRequestCode()
                            1 -> onVerifyCode()
                            else -> onSubmitPassword(password, confirmation)
                        }
                    },
                    isEnabled =
                        !isLoading &&
                            when (step) {
                                0 -> {
                                    email.isNotBlank() && resendSeconds == 0
                                }

                                1 -> {
                                    code.length == 6 && codeRemainingSeconds > 0
                                }

                                else -> {
                                    password.isNotBlank() &&
                                        confirmation.isNotBlank() && resetTokenRemainingSeconds > 0
                                }
                            },
                )
            }
        }
    }
}

private fun PasswordResetError.toStringResource(): Int =
    when (this) {
        PasswordResetError.Validation -> R.string.auth_password_reset_validation
        PasswordResetError.InvalidCode -> R.string.auth_password_reset_code_invalid
        PasswordResetError.InvalidToken -> R.string.auth_password_reset_token_invalid
        PasswordResetError.RateLimited -> R.string.auth_password_reset_rate_limited
        PasswordResetError.ServiceUnavailable -> R.string.auth_password_reset_service_unavailable
        PasswordResetError.Network, PasswordResetError.Generic -> R.string.auth_password_reset_generic_error
    }
