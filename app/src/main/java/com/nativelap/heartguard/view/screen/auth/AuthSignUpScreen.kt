package com.nativelap.heartguard.view.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.auth.AuthPrimaryButton
import com.nativelap.heartguard.view.component.auth.AuthTextField
import com.nativelap.heartguard.view.component.auth.AuthTitleBlock

/** 이메일 인증코드와 앱 계정 자격 증명을 입력받는다. */
@Composable
internal fun AuthSignUpScreen(
    email: String,
    verificationCode: String,
    password: String,
    passwordConfirmation: String,
    isSendingEmailCode: Boolean,
    isEmailCodeSent: Boolean,
    resendCooldownSeconds: Int,
    isSubmitting: Boolean,
    statusMessage: String?,
    onEvent: (AuthSignUpScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var hasFormFocus by remember { mutableStateOf(false) }
    val isKeyboardVisible = hasFormFocus && WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val pageScrollState = rememberScrollState()
    val formScrollState = rememberScrollState()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.extraColors.authBackground,
        contentWindowInsets = WindowInsets.statusBars,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
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
                                .widthIn(max = HeartGuardComponentSize.AuthSignUpContentMaxWidth)
                                .fillMaxWidth()
                                .padding(top = HeartGuardSpacing.AuthSignUpTop),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        AuthTitleBlock(
                            title = stringResource(R.string.auth_signup_title),
                            description = stringResource(R.string.auth_signup_description),
                        )

                        Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpTitleForm))

                        AuthTextField(
                            label = stringResource(R.string.auth_email),
                            text = email,
                            onTextChange = { value ->
                                onEvent(AuthSignUpScreenEvent.EmailChanged(value))
                            },
                            placeholder = stringResource(R.string.auth_email_hint),
                            minimumHeight = HeartGuardComponentSize.TouchTarget,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            trailingIcon = {
                                TextButton(
                                    onClick = {
                                        onEvent(AuthSignUpScreenEvent.RequestEmailCodeClicked)
                                    },
                                    modifier =
                                        Modifier
                                            .padding(end = EMAIL_CODE_BUTTON_END_PADDING),
                                    enabled =
                                        !isSendingEmailCode &&
                                            !isSubmitting &&
                                            resendCooldownSeconds == 0,
                                    contentPadding =
                                        PaddingValues(
                                            horizontal = EMAIL_CODE_BUTTON_HORIZONTAL_PADDING,
                                        ),
                                ) {
                                    Text(
                                        text =
                                            if (resendCooldownSeconds > 0) {
                                                stringResource(
                                                    R.string.auth_signup_resend_wait,
                                                    resendCooldownSeconds,
                                                )
                                            } else {
                                                stringResource(
                                                    if (isEmailCodeSent) {
                                                        R.string.auth_signup_resend_code
                                                    } else {
                                                        R.string.auth_signup_send_code
                                                    },
                                                )
                                            },
                                        color = MaterialTheme.extraColors.authPrimary,
                                        style =
                                            MaterialTheme.typography.labelLarge.copy(
                                                fontSize = EMAIL_CODE_BUTTON_FONT_SIZE,
                                                lineHeight = EMAIL_CODE_BUTTON_LINE_HEIGHT,
                                                fontWeight = FontWeight.SemiBold,
                                            ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            },
                        )

                        Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpEmailCodeGap))

                        AuthTextField(
                            label = stringResource(R.string.auth_signup_verification_code_label),
                            text = verificationCode,
                            onTextChange = { value ->
                                onEvent(
                                    AuthSignUpScreenEvent.VerificationCodeChanged(
                                        value.filter(Char::isDigit).take(6),
                                    ),
                                )
                            },
                            placeholder = stringResource(R.string.auth_signup_verification_code_hint),
                            minimumHeight = HeartGuardComponentSize.TouchTarget,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        )

                        Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpFieldGroup))

                        AuthTextField(
                            label = stringResource(R.string.auth_password),
                            text = password,
                            onTextChange = { value ->
                                onEvent(AuthSignUpScreenEvent.PasswordChanged(value))
                            },
                            placeholder = stringResource(R.string.auth_signup_password_hint),
                            minimumHeight = HeartGuardComponentSize.TouchTarget,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = PasswordVisualTransformation(),
                        )

                        Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpFieldGroup))

                        AuthTextField(
                            label = stringResource(R.string.auth_signup_password_confirmation_label),
                            text = passwordConfirmation,
                            onTextChange = { value ->
                                onEvent(AuthSignUpScreenEvent.PasswordConfirmationChanged(value))
                            },
                            placeholder = stringResource(R.string.auth_signup_password_confirmation_hint),
                            minimumHeight = HeartGuardComponentSize.TouchTarget,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = PasswordVisualTransformation(),
                        )
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
                        ).widthIn(max = HeartGuardComponentSize.AuthActionMaxWidth)
                        .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpActionsTop))

                AuthPrimaryButton(
                    title = stringResource(R.string.auth_signup),
                    onClick = {
                        onEvent(AuthSignUpScreenEvent.SignUpClicked)
                    },
                    isEnabled = !isSubmitting && !isSendingEmailCode,
                )

                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(HeartGuardSpacing.Compact))
                    Text(
                        text = statusMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = HeartGuardSpacing.AuthFooterGap),
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
        }
    }
}

private val EMAIL_CODE_BUTTON_END_PADDING = 4.dp
private val EMAIL_CODE_BUTTON_HORIZONTAL_PADDING = 2.dp
private val EMAIL_CODE_BUTTON_FONT_SIZE = 13.sp
private val EMAIL_CODE_BUTTON_LINE_HEIGHT = 18.sp

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun AuthSignUpScreenPreview() {
    HeartGuardTheme {
        AuthSignUpScreen(
            email = "",
            verificationCode = "",
            password = "",
            passwordConfirmation = "",
            isSendingEmailCode = false,
            isEmailCodeSent = false,
            resendCooldownSeconds = 0,
            isSubmitting = false,
            statusMessage = null,
            onEvent = {},
        )
    }
}
