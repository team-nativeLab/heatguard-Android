package com.nativelap.heartguard.view.screen.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.auth.AuthPrimaryButton
import com.nativelap.heartguard.view.component.auth.AuthPrompt
import com.nativelap.heartguard.view.component.auth.AuthTextField
import com.nativelap.heartguard.view.component.auth.AuthTitleBlock
import com.nativelap.heartguard.ui.theme.extraColors

/** 회원가입 화면을 이름·이메일·비밀번호 입력 Component로 구성한다. */
@Composable
fun AuthSignUpScreen(
    companyName: String,
    name: String,
    email: String,
    password: String,
    passwordConfirmation: String,
    onCompanyNameChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordConfirmationChange: (String) -> Unit,
    onSignUpClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSignUpEnabled: Boolean = true,
    isPasswordMismatch: Boolean = false,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.extraColors.authBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = HeartGuardSpacing.AuthHorizontal)
                .imePadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = HeartGuardComponentSize.AuthSignUpContentMaxWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpTop))

                AuthTitleBlock(
                    title = stringResource(R.string.auth_signup_title),
                    description = stringResource(R.string.auth_signup_description),
                    titleFontSize = HeartGuardFontSize.AuthSignUpTitle,
                    titleLineHeight = HeartGuardFontSize.AuthSignUpTitleLineHeight,
                    descriptionTopPadding = HeartGuardSpacing.AuthSignUpTitleDescription,
                )

                Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpTitleForm))

                AuthTextField(
                    label = stringResource(R.string.auth_company),
                    text = companyName,
                    onTextChange = onCompanyNameChange,
                    placeholder = stringResource(R.string.auth_company_hint),
                )

                Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpFieldGroup))

                AuthTextField(
                    label = stringResource(R.string.auth_name),
                    text = name,
                    onTextChange = onNameChange,
                    placeholder = stringResource(R.string.auth_name_hint),
                )

                Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpFieldGroup))

                AuthTextField(
                    label = stringResource(R.string.auth_email),
                    text = email,
                    onTextChange = onEmailChange,
                    placeholder = stringResource(R.string.auth_email_hint),
                )

                Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpFieldGroup))

                AuthTextField(
                    label = stringResource(R.string.auth_password),
                    text = password,
                    onTextChange = onPasswordChange,
                    placeholder = stringResource(R.string.auth_password_hint),
                    visualTransformation = PasswordVisualTransformation(),
                )

                Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpFieldGroup))

                AuthTextField(
                    label = stringResource(R.string.auth_password_confirm),
                    text = passwordConfirmation,
                    onTextChange = onPasswordConfirmationChange,
                    placeholder = stringResource(R.string.auth_password_confirm_hint),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = isPasswordMismatch,
                    supportingText = if (isPasswordMismatch) {
                        stringResource(R.string.auth_password_mismatch_error)
                    } else {
                        null
                    },
                )
            }

            Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpFormButton))

            Column(
                modifier = Modifier
                    .widthIn(max = HeartGuardComponentSize.AuthActionMaxWidth)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AuthPrimaryButton(
                    title = stringResource(R.string.auth_signup_submit),
                    onClick = onSignUpClick,
                    isEnabled = isSignUpEnabled,
                )

                Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthButtonPrompt))

                AuthPrompt(
                    message = stringResource(R.string.auth_login_prompt),
                    actionTitle = stringResource(R.string.auth_login),
                    onActionClick = onLoginClick,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun AuthSignUpScreenPreview() {
    HeartGuardTheme {
        AuthSignUpScreen(
            companyName = "",
            name = "",
            email = "",
            password = "",
            passwordConfirmation = "",
            onCompanyNameChange = {},
            onNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onPasswordConfirmationChange = {},
            onSignUpClick = {},
            onLoginClick = {},
        )
    }
}
