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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.account.WithdrawTopBar
import com.nativelap.heartguard.view.component.auth.AuthPrimaryButton
import com.nativelap.heartguard.view.component.auth.AuthTextField
import com.nativelap.heartguard.view.component.auth.AuthTitleBlock
import com.nativelap.heartguard.view.component.auth.AuthVerificationCodeInput
import com.nativelap.heartguard.view.component.password.PasswordInputField

/** 이메일 인증코드와 Team 계정 가입 정보를 입력받는다. */
@Composable
internal fun AuthSignUpScreen(
    name: String,
    email: String,
    verificationCode: String,
    password: String,
    passwordConfirmation: String,
    isSendingEmailCode: Boolean,
    isEmailCodeSent: Boolean,
    isEmailCodeResendBlocked: Boolean,
    emailCodeRemainingSeconds: Int,
    isSubmitting: Boolean,
    statusMessage: String?,
    isStatusError: Boolean,
    step: Int,
    onEvent: (AuthSignUpScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var hasFormFocus by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isPasswordConfirmationVisible by remember { mutableStateOf(false) }
    val isKeyboardVisible = hasFormFocus && WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val pageScrollState = rememberScrollState()
    val formScrollState = rememberScrollState()
    val primaryButtonEvent =
        when (step) {
            SIGN_UP_EMAIL_STEP -> {
                if (isEmailCodeSent) {
                    AuthSignUpScreenEvent.ContinueClicked
                } else {
                    AuthSignUpScreenEvent.RequestEmailCodeClicked
                }
            }

            SIGN_UP_CODE_STEP -> {
                AuthSignUpScreenEvent.ContinueClicked
            }

            else -> {
                AuthSignUpScreenEvent.SignUpClicked
            }
        }
    val isPrimaryButtonEnabled =
        !isSubmitting &&
            !isSendingEmailCode &&
            (step != SIGN_UP_CODE_STEP || verificationCode.length == 6) &&
            (step != SIGN_UP_CODE_STEP || emailCodeRemainingSeconds > 0) &&
            (step != SIGN_UP_PASSWORD_STEP || isPasswordFormValid(password, passwordConfirmation)) &&
            (step != SIGN_UP_EMAIL_STEP || isEmailCodeSent || !isEmailCodeResendBlocked)

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.extraColors.pageBackground,
        contentWindowInsets = WindowInsets.statusBars,
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier =
                    Modifier
                        .widthIn(max = HeartGuardComponentSize.PageContentMaxWidth)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(horizontal = HeartGuardSpacing.AuthHorizontal)
                        .windowInsetsPadding(
                            if (hasFormFocus) {
                                WindowInsets.navigationBars.union(WindowInsets.ime)
                            } else {
                                WindowInsets.navigationBars
                            },
                        ).onFocusChanged { focusState -> hasFormFocus = focusState.hasFocus }
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
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = HeartGuardSpacing.AuthSignUpTop),
                            horizontalAlignment = Alignment.Start,
                        ) {
                            if (!isSubmitting && !isSendingEmailCode) {
                                WithdrawTopBar(
                                    title = stringResource(R.string.auth_signup_title),
                                    backContentDescription = stringResource(R.string.common_back_description),
                                    onBackClick = {
                                        if (step == SIGN_UP_EMAIL_STEP) {
                                            onEvent(AuthSignUpScreenEvent.LoginClicked)
                                        } else {
                                            onEvent(AuthSignUpScreenEvent.StepBackClicked)
                                        }
                                    },
                                )
                            }

                            Spacer(modifier = Modifier.height(HeartGuardSpacing.Item))
                            SignUpStepProgress(step = step)

                            Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpTitleForm))

                            AuthTitleBlock(
                                title = stringResource(signUpStepTitleResource(step)),
                                description =
                                    if (step == SIGN_UP_CODE_STEP) {
                                        stringResource(
                                            R.string.auth_signup_code_step_description,
                                            email,
                                        )
                                    } else {
                                        stringResource(signUpStepDescriptionResource(step))
                                    },
                            )

                            Spacer(modifier = Modifier.height(HeartGuardSpacing.Item))

                            if (step == SIGN_UP_EMAIL_STEP) {
                                AuthTextField(
                                    label = stringResource(R.string.auth_signup_name_label),
                                    text = name,
                                    onTextChange = { value ->
                                        onEvent(AuthSignUpScreenEvent.NameChanged(value))
                                    },
                                    enabled = !isSubmitting && !isSendingEmailCode,
                                    placeholder = stringResource(R.string.auth_signup_name_hint),
                                    minimumHeight = HeartGuardComponentSize.TouchTarget,
                                )
                                Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpFieldGroup))
                                AuthTextField(
                                    label = stringResource(R.string.auth_email),
                                    text = email,
                                    onTextChange = { value ->
                                        onEvent(AuthSignUpScreenEvent.EmailChanged(value))
                                    },
                                    enabled = !isSubmitting && !isSendingEmailCode,
                                    placeholder = stringResource(R.string.auth_email_hint),
                                    minimumHeight = HeartGuardComponentSize.TouchTarget,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                )
                            }

                            if (step == SIGN_UP_EMAIL_STEP) {
                                Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpFieldGroup))
                            }

                            if (step == SIGN_UP_CODE_STEP) {
                                AuthVerificationCodeInput(
                                    code = verificationCode,
                                    onCodeChange = { code ->
                                        onEvent(AuthSignUpScreenEvent.VerificationCodeChanged(code))
                                    },
                                    enabled = !isSubmitting && !isSendingEmailCode && emailCodeRemainingSeconds > 0,
                                )
                            }

                            if (step == SIGN_UP_CODE_STEP) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text =
                                            stringResource(
                                                R.string.auth_signup_code_remaining_time,
                                                emailCodeRemainingSeconds / SECONDS_PER_MINUTE,
                                                emailCodeRemainingSeconds % SECONDS_PER_MINUTE,
                                            ),
                                        color = MaterialTheme.extraColors.secondaryText,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    TextButton(
                                        onClick = { onEvent(AuthSignUpScreenEvent.RequestEmailCodeClicked) },
                                        enabled =
                                            !isSendingEmailCode && !isSubmitting &&
                                                !isEmailCodeResendBlocked,
                                        modifier = Modifier.heightIn(min = HeartGuardComponentSize.TouchTarget),
                                    ) {
                                        Text(stringResource(R.string.auth_signup_resend_code_action))
                                    }
                                }
                                Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpFieldGroup))
                            }

                            if (step == SIGN_UP_PASSWORD_STEP) {
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.extraColors.authInputBackground,
                                                RoundedCornerShape(HeartGuardRadius.InputBox),
                                            ).padding(HeartGuardSpacing.Item),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Column {
                                        Text(
                                            text = stringResource(R.string.auth_signup_email_verified),
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                        Text(
                                            text = email,
                                            color = MaterialTheme.extraColors.authOnSurface,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpFieldGroup))
                                PasswordInputField(
                                    label = stringResource(R.string.auth_password),
                                    password = password,
                                    onPasswordChange = { value ->
                                        onEvent(AuthSignUpScreenEvent.PasswordChanged(value))
                                    },
                                    onVisibilityClick = { isPasswordVisible = !isPasswordVisible },
                                    isPasswordVisible = isPasswordVisible,
                                    placeholder = stringResource(R.string.auth_signup_password_hint),
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isSubmitting,
                                )
                                Text(
                                    text = stringResource(R.string.auth_signup_password_requirement),
                                    color = MaterialTheme.extraColors.secondaryText,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = HeartGuardSpacing.Compact),
                                )
                            }

                            if (step == SIGN_UP_PASSWORD_STEP) {
                                Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpFieldGroup))
                            }

                            if (step == SIGN_UP_PASSWORD_STEP) {
                                PasswordInputField(
                                    label = stringResource(R.string.auth_signup_password_confirmation_label),
                                    password = passwordConfirmation,
                                    onPasswordChange = { value ->
                                        onEvent(AuthSignUpScreenEvent.PasswordConfirmationChanged(value))
                                    },
                                    onVisibilityClick = {
                                        isPasswordConfirmationVisible = !isPasswordConfirmationVisible
                                    },
                                    isPasswordVisible = isPasswordConfirmationVisible,
                                    placeholder = stringResource(R.string.auth_signup_password_confirmation_hint),
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isSubmitting,
                                )
                            }
                        }
                    }
                }
                Column(
                    modifier =
                        Modifier
                            .padding(
                                top =
                                    if (isKeyboardVisible) {
                                        HeartGuardSpacing.Item
                                    } else {
                                        0.dp
                                    },
                            ).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpActionsTop))

                    if (step == SIGN_UP_EMAIL_STEP) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = HeartGuardSpacing.AuthFooterGap),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.auth_signup_existing_account),
                                color = MaterialTheme.extraColors.authOnSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            TextButton(
                                onClick = {
                                    onEvent(AuthSignUpScreenEvent.LoginClicked)
                                },
                            ) {
                                Text(
                                    text = stringResource(R.string.auth_login),
                                    color = MaterialTheme.extraColors.authPrimary,
                                    style =
                                        MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                        ),
                                )
                            }
                        }
                    }

                    AuthPrimaryButton(
                        title =
                            stringResource(
                                when (step) {
                                    SIGN_UP_EMAIL_STEP -> {
                                        if (isEmailCodeSent) {
                                            R.string.auth_signup_next
                                        } else {
                                            R.string.auth_signup_send_code
                                        }
                                    }

                                    SIGN_UP_CODE_STEP -> {
                                        R.string.auth_signup_verify
                                    }

                                    else -> {
                                        R.string.auth_signup_complete
                                    }
                                },
                            ),
                        onClick = { onEvent(primaryButtonEvent) },
                        modifier =
                            Modifier
                                .heightIn(min = HeartGuardComponentSize.TouchTarget)
                                .padding(horizontal = HeartGuardSpacing.Tight),
                        isEnabled = isPrimaryButtonEnabled,
                    )

                    if (statusMessage != null) {
                        Spacer(Modifier.height(HeartGuardSpacing.Compact))
                        Text(
                            text = statusMessage,
                            color =
                                if (isStatusError) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.extraColors.secondaryText
                                },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

private fun signUpStepTitleResource(step: Int): Int =
    when (step) {
        SIGN_UP_EMAIL_STEP -> R.string.auth_signup_email_step_title
        SIGN_UP_CODE_STEP -> R.string.auth_signup_code_step_title
        else -> R.string.auth_signup_password_step_title
    }

private fun signUpStepDescriptionResource(step: Int): Int =
    when (step) {
        SIGN_UP_EMAIL_STEP -> R.string.auth_signup_email_step_description
        SIGN_UP_CODE_STEP -> R.string.auth_signup_code_step_description
        else -> R.string.auth_signup_password_step_description
    }

@Composable
private fun SignUpStepProgress(step: Int) {
    val labels =
        listOf(
            stringResource(R.string.auth_signup_step_email),
            stringResource(R.string.auth_signup_step_code),
            stringResource(R.string.auth_signup_step_password),
        )
    Column(verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact)) {
        Row(horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight)) {
            repeat(SIGN_UP_STEP_COUNT) { index ->
                val progress by animateFloatAsState(
                    targetValue =
                        if (index < step) {
                            1f
                        } else if (index == step) {
                            1f
                        } else {
                            0f
                        },
                    animationSpec =
                        tween(
                            durationMillis = SIGN_UP_PROGRESS_ANIMATION_MILLIS,
                            easing = FastOutSlowInEasing,
                        ),
                    label = "signup_step_$index",
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
                        modifier =
                            Modifier
                                .fillMaxWidth(progress)
                                .height(4.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)),
                    )
                }
            }
        }
        Text(
            text = labels[step],
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun AuthSignUpScreenPreview() {
    HeartGuardTheme {
        AuthSignUpScreen(
            name = "",
            email = "",
            verificationCode = "",
            password = "",
            passwordConfirmation = "",
            isSendingEmailCode = false,
            isEmailCodeSent = false,
            isEmailCodeResendBlocked = false,
            emailCodeRemainingSeconds = 600,
            isSubmitting = false,
            statusMessage = null,
            isStatusError = false,
            step = SIGN_UP_EMAIL_STEP,
            onEvent = {},
        )
    }
}

private const val SIGN_UP_EMAIL_STEP = 0
private const val SIGN_UP_CODE_STEP = 1
private const val SIGN_UP_PASSWORD_STEP = 2
private const val SIGN_UP_STEP_COUNT = 3
private const val SIGN_UP_PROGRESS_ANIMATION_MILLIS = 450
private const val SECONDS_PER_MINUTE = 60

private fun isPasswordFormValid(
    password: String,
    confirmation: String,
): Boolean =
    password.length >= MIN_PASSWORD_LENGTH &&
        password.any { character -> character in 'A'..'Z' || character in 'a'..'z' } &&
        password.any { character -> character in '0'..'9' } &&
        password == confirmation

private const val MIN_PASSWORD_LENGTH = 8
