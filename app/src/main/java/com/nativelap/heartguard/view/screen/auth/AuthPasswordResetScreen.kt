package com.nativelap.heartguard.view.screen.auth

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetError
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.account.WithdrawTopBar
import com.nativelap.heartguard.view.component.auth.AuthPrimaryButton
import com.nativelap.heartguard.view.component.auth.AuthTextField
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
    var hasFormFocus by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmationVisible by remember { mutableStateOf(false) }
    val isKeyboardVisible = hasFormFocus && WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val pageScrollState = rememberScrollState()
    val formScrollState = rememberScrollState()
    val errorMessage = failure?.let { error -> stringResource(error.toStringResource()) }
    LaunchedEffect(step) {
        if (step < PASSWORD_RESET_PASSWORD_STEP) {
            password = ""
            confirmation = ""
            passwordVisible = false
            confirmationVisible = false
        }
    }
    Scaffold(
        containerColor = MaterialTheme.extraColors.pageBackground,
        contentWindowInsets = WindowInsets.statusBars,
    ) { insets ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(insets)
                    .consumeWindowInsets(insets)
                    .widthIn(max = HeartGuardComponentSize.PageContentMaxWidth)
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        if (hasFormFocus) {
                            WindowInsets.navigationBars.union(WindowInsets.ime)
                        } else {
                            WindowInsets.navigationBars
                        },
                    ).onFocusChanged { hasFormFocus = it.hasFocus }
                    .then(
                        if (isKeyboardVisible) {
                            Modifier.verticalScroll(pageScrollState)
                        } else {
                            Modifier
                        },
                    ),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!isLoading) {
                WithdrawTopBar(
                    title = stringResource(R.string.auth_password_reset_title),
                    backContentDescription = stringResource(R.string.common_back_description),
                    onBackClick = onBackClick,
                )
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HeartGuardSpacing.AuthHorizontal)
                        .then(
                            if (isKeyboardVisible) {
                                Modifier
                            } else {
                                Modifier.weight(1f)
                            },
                        ),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .then(
                                if (isKeyboardVisible) {
                                    Modifier
                                } else {
                                    Modifier.verticalScroll(formScrollState)
                                },
                            ),
                ) {
                    Spacer(modifier = Modifier.height(HeartGuardSpacing.Item))
                    PasswordResetStepProgress(step)
                    Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpTitleForm))
                    AuthTitleBlock(
                        title = stringResource(passwordResetStepTitleResource(step)),
                        description = stringResource(passwordResetStepDescriptionResource(step)),
                    )
                    Spacer(modifier = Modifier.height(HeartGuardSpacing.Item))
                    when (step) {
                        PASSWORD_RESET_EMAIL_STEP -> {
                            AuthTextField(
                                label = stringResource(R.string.auth_password_reset_email_label),
                                text = email,
                                onTextChange = onEmailChange,
                                placeholder = stringResource(R.string.auth_password_reset_email_hint),
                                enabled = !isLoading,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                minimumHeight = HeartGuardComponentSize.TouchTarget,
                            )
                            if (errorMessage != null) {
                                PasswordResetErrorMessage(errorMessage)
                            }
                        }

                        PASSWORD_RESET_CODE_STEP -> {
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpFieldGroup))
                            AuthVerificationCodeInput(
                                code = code,
                                onCodeChange = onCodeChange,
                                enabled = !isLoading && codeRemainingSeconds > 0,
                                errorDescription = errorMessage,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text =
                                        if (codeRemainingSeconds > 0) {
                                            "${codeRemainingSeconds / 60}:" +
                                                (codeRemainingSeconds % 60).toString().padStart(2, '0')
                                        } else {
                                            stringResource(R.string.auth_password_reset_code_expired)
                                        },
                                    color = MaterialTheme.extraColors.authOnSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                TextButton(
                                    onClick = onRequestCode,
                                    enabled = !isLoading && resendSeconds == 0,
                                    modifier = Modifier.heightIn(min = HeartGuardComponentSize.TouchTarget),
                                ) {
                                    Text(
                                        if (resendSeconds > 0) {
                                            stringResource(R.string.auth_password_reset_resend_waiting, resendSeconds)
                                        } else {
                                            stringResource(R.string.auth_password_reset_resend)
                                        },
                                    )
                                }
                            }
                            if (errorMessage != null) {
                                PasswordResetErrorMessage(errorMessage)
                            }
                        }

                        else -> {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(HeartGuardRadius.InputBox),
                                        ).padding(HeartGuardSpacing.Item),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                )
                                Column {
                                    Text(
                                        stringResource(R.string.auth_signup_email_verified),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                    Text(
                                        email,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpFieldGroup))
                            PasswordInputField(
                                label = stringResource(R.string.auth_password_reset_password_label),
                                password = password,
                                placeholder = stringResource(R.string.auth_password_reset_password_hint),
                                isPasswordVisible = passwordVisible,
                                onPasswordChange = { password = it },
                                onVisibilityClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading,
                            )
                            Text(
                                text = stringResource(R.string.auth_signup_password_requirement),
                                color = MaterialTheme.extraColors.secondaryText,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = HeartGuardSpacing.Compact),
                            )
                            Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpFieldGroup))
                            PasswordInputField(
                                label = stringResource(R.string.auth_password_reset_confirm_label),
                                password = confirmation,
                                placeholder = stringResource(R.string.auth_password_reset_confirm_hint),
                                isPasswordVisible = confirmationVisible,
                                onPasswordChange = { confirmation = it },
                                onVisibilityClick = { confirmationVisible = !confirmationVisible },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading,
                            )
                            if (errorMessage != null) {
                                PasswordResetErrorMessage(errorMessage)
                            }
                            Text(
                                text =
                                    stringResource(
                                        R.string.auth_password_reset_token_remaining,
                                        resetTokenRemainingSeconds / 60,
                                        resetTokenRemainingSeconds % 60,
                                    ),
                                color = MaterialTheme.extraColors.authOnSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                modifier =
                                    Modifier.padding(
                                        top = HeartGuardSpacing.AuthSignUpFieldGroup,
                                    ),
                            )
                        }
                    }
                }
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HeartGuardSpacing.AuthHorizontal)
                        .padding(bottom = HeartGuardSpacing.AuthActionBottom)
                        .widthIn(max = HeartGuardComponentSize.AuthActionMaxWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpActionsTop))
                AuthPrimaryButton(
                    title = stringResource(passwordResetActionResource(step)),
                    onClick = {
                        when (step) {
                            PASSWORD_RESET_EMAIL_STEP -> onRequestCode()
                            PASSWORD_RESET_CODE_STEP -> onVerifyCode()
                            else -> onSubmitPassword(password, confirmation)
                        }
                    },
                    modifier = Modifier.heightIn(min = HeartGuardComponentSize.TouchTarget),
                    isEnabled =
                        !isLoading &&
                            when (step) {
                                PASSWORD_RESET_EMAIL_STEP -> {
                                    email.isNotBlank() && resendSeconds == 0
                                }

                                PASSWORD_RESET_CODE_STEP -> {
                                    code.length == 6 && codeRemainingSeconds > 0
                                }

                                else -> {
                                    password.isNotBlank() && confirmation.isNotBlank() &&
                                        resetTokenRemainingSeconds > 0
                                }
                            },
                )
            }
        }
    }
}

@Composable
private fun PasswordResetErrorMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier.padding(top = HeartGuardSpacing.AuthErrorMessageGap),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun PasswordResetStepProgress(step: Int) {
    val labels =
        listOf(
            stringResource(R.string.auth_password_reset_step_email),
            stringResource(R.string.auth_password_reset_step_code),
            stringResource(R.string.auth_password_reset_step_password),
        )
    Column(
        verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
        ) {
            repeat(PASSWORD_RESET_STEP_COUNT) { index ->
                val progress by animateFloatAsState(
                    targetValue = if (index <= step) 1f else 0f,
                    animationSpec =
                        tween(
                            PASSWORD_RESET_PROGRESS_ANIMATION_MILLIS,
                            easing = FastOutSlowInEasing,
                        ),
                    label = "password_reset_step_$index",
                )
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(
                                MaterialTheme.extraColors.cardBorder,
                                RoundedCornerShape(4.dp),
                            ),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(4.dp),
                            ),
                    )
                }
            }
        }
        Text(
            text = labels[step.coerceIn(0, PASSWORD_RESET_STEP_COUNT - 1)],
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private fun passwordResetStepTitleResource(step: Int): Int =
    when (step) {
        PASSWORD_RESET_EMAIL_STEP -> R.string.auth_password_reset_email_title
        PASSWORD_RESET_CODE_STEP -> R.string.auth_password_reset_code_title
        else -> R.string.auth_password_reset_password_title
    }

private fun passwordResetStepDescriptionResource(step: Int): Int =
    when (step) {
        PASSWORD_RESET_EMAIL_STEP -> R.string.auth_password_reset_email_step_description
        PASSWORD_RESET_CODE_STEP -> R.string.auth_password_reset_code_step_description
        else -> R.string.auth_password_reset_password_step_description
    }

private fun passwordResetActionResource(step: Int): Int =
    when (step) {
        PASSWORD_RESET_EMAIL_STEP -> R.string.auth_password_reset_send
        PASSWORD_RESET_CODE_STEP -> R.string.password_reset_verify
        else -> R.string.auth_password_reset_submit
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

private const val PASSWORD_RESET_EMAIL_STEP = 0
private const val PASSWORD_RESET_CODE_STEP = 1
private const val PASSWORD_RESET_PASSWORD_STEP = 2
private const val PASSWORD_RESET_STEP_COUNT = 3
private const val PASSWORD_RESET_PROGRESS_ANIMATION_MILLIS = 450
