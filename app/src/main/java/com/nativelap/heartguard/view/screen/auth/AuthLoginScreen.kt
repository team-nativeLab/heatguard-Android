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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.auth.AuthPrimaryButton
import com.nativelap.heartguard.view.component.auth.AuthPrompt
import com.nativelap.heartguard.view.component.auth.AuthTextField
import com.nativelap.heartguard.view.component.auth.AuthTitleBlock
import com.nativelap.heartguard.view.component.brand.BrandMark
import com.nativelap.heartguard.ui.theme.extraColors

/** 이메일 로그인 화면을 기존 인증 Component 조합으로 구성한다. */
@Composable
fun AuthLoginScreen(
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPasswordError: Boolean = false,
    passwordErrorMessage: String? = null,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.extraColors.authBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = HeartGuardSpacing.AuthHorizontal)
                .imePadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = HeartGuardComponentSize.AuthContentMaxWidth)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthTop))

                    BrandMark(
                        brandPainter = painterResource(R.drawable.heart_guard_logo),
                        contentDescription = stringResource(R.string.brand_name),
                    )

                    Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthLogoTitle))

                    AuthTitleBlock(
                        title = stringResource(R.string.auth_login_title),
                        description = stringResource(R.string.auth_login_description),
                    )

                    Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthTitleForm))

                    AuthTextField(
                        label = stringResource(R.string.auth_email),
                        text = email,
                        onTextChange = onEmailChange,
                        placeholder = stringResource(R.string.auth_email_hint),
                    )

                    Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthFieldGroup))

                    AuthTextField(
                        label = stringResource(R.string.auth_password),
                        text = password,
                        onTextChange = onPasswordChange,
                        placeholder = stringResource(R.string.auth_password_hint),
                        isError = isPasswordError,
                        supportingText = passwordErrorMessage,
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = HeartGuardComponentSize.AuthActionMaxWidth)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AuthPrimaryButton(
                    title = stringResource(R.string.auth_login),
                    onClick = onLoginClick,
                )

                Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthButtonPrompt))

                AuthPrompt(
                    message = stringResource(R.string.auth_signup_prompt),
                    actionTitle = stringResource(R.string.auth_signup),
                    onActionClick = onSignUpClick,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun AuthLoginScreenPreview() {
    HeartGuardTheme {
        AuthLoginScreen(
            email = "",
            password = "",
            onEmailChange = {},
            onPasswordChange = {},
            onLoginClick = {},
            onSignUpClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun AuthLoginScreenErrorPreview() {
    HeartGuardTheme {
        AuthLoginScreen(
            email = "worker@heartguard.com",
            password = "wrong-password",
            onEmailChange = {},
            onPasswordChange = {},
            onLoginClick = {},
            onSignUpClick = {},
            isPasswordError = true,
            passwordErrorMessage = stringResource(R.string.auth_password_error),
        )
    }
}
