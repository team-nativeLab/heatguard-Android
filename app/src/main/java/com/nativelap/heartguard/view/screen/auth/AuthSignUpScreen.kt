package com.nativelap.heartguard.view.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.auth.AuthPrimaryButton
import com.nativelap.heartguard.view.component.auth.AuthPrompt
import com.nativelap.heartguard.view.component.auth.AuthTextField
import com.nativelap.heartguard.view.component.auth.AuthTitleBlock
import com.nativelap.heartguard.view.component.brand.BrandMark

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
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = HeartGuardSpacing.AuthHorizontal)
                .padding(vertical = HeartGuardSpacing.Section),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = AUTH_SIGN_UP_CONTENT_MAX_WIDTH),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
            ) {
                BrandMark(
                    brandPainter = painterResource(R.drawable.heart_guard_logo),
                    contentDescription = stringResource(R.string.brand_name),
                )

                Spacer(modifier = Modifier.height(HeartGuardSpacing.Section))

                AuthTitleBlock(
                    title = stringResource(R.string.auth_signup_title),
                    description = stringResource(R.string.auth_signup_description),
                )

                Spacer(modifier = Modifier.height(HeartGuardSpacing.Section))

                AuthTextField(
                    label = stringResource(R.string.auth_company),
                    text = companyName,
                    onTextChange = onCompanyNameChange,
                    placeholder = stringResource(R.string.auth_company_hint),
                )

                Spacer(modifier = Modifier.height(HeartGuardSpacing.Item))

                AuthTextField(
                    label = stringResource(R.string.auth_name),
                    text = name,
                    onTextChange = onNameChange,
                    placeholder = stringResource(R.string.auth_name_hint),
                )

                Spacer(modifier = Modifier.height(HeartGuardSpacing.Item))

                AuthTextField(
                    label = stringResource(R.string.auth_email),
                    text = email,
                    onTextChange = onEmailChange,
                    placeholder = stringResource(R.string.auth_email_hint),
                )

                Spacer(modifier = Modifier.height(HeartGuardSpacing.Item))

                AuthTextField(
                    label = stringResource(R.string.auth_password),
                    text = password,
                    onTextChange = onPasswordChange,
                    placeholder = stringResource(R.string.auth_password_hint),
                    visualTransformation = PasswordVisualTransformation(),
                )

                Spacer(modifier = Modifier.height(HeartGuardSpacing.Item))

                AuthTextField(
                    label = stringResource(R.string.auth_password_confirm),
                    text = passwordConfirmation,
                    onTextChange = onPasswordConfirmationChange,
                    placeholder = stringResource(R.string.auth_password_confirm_hint),
                    visualTransformation = PasswordVisualTransformation(),
                )

                Spacer(modifier = Modifier.height(HeartGuardSpacing.Section))

                AuthPrimaryButton(
                    title = stringResource(R.string.auth_signup_submit),
                    onClick = onSignUpClick,
                    isEnabled = isSignUpEnabled,
                )

                Spacer(modifier = Modifier.height(HeartGuardSpacing.Item))

                AuthPrompt(
                    message = stringResource(R.string.auth_login_prompt),
                    actionTitle = stringResource(R.string.auth_login),
                    onActionClick = onLoginClick,
                )
            }
        }
    }
}

private val AUTH_SIGN_UP_CONTENT_MAX_WIDTH = 286.dp

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
