package com.nativelap.heartguard.view.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.auth.AuthPrimaryButton
import com.nativelap.heartguard.view.component.auth.AuthTextField
import com.nativelap.heartguard.view.component.auth.AuthTitleBlock
import com.nativelap.heartguard.view.component.brand.BrandMark

/** 이메일 로그인 화면을 기존 인증 Component 조합으로 구성한다. */
@Composable
fun AuthLoginScreen(
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPasswordError: Boolean = false,
    passwordErrorMessage: String? = null,
    statusMessage: String? = null,
    isSubmitting: Boolean = false,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.extraColors.authBackground,
        contentWindowInsets = WindowInsets.statusBars,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .padding(horizontal = HeartGuardSpacing.AuthHorizontal)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
            // 입력 폼과 로그인 동작을 화면 위아래로 분리해 키보드가 열려도 입력 흐름을 유지한다.
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                var formContentHeight by remember { mutableIntStateOf(0) }
                val topSpacing = if (formContentHeight == 0) {
                    0.dp
                } else {
                    with(LocalDensity.current) {
                        val maximumTopSpacing = (HeartGuardSpacing.AuthTop - innerPadding.calculateTopPadding())
                            .coerceAtLeast(0.dp)
                        (maxHeight - formContentHeight.toDp()).coerceIn(0.dp, maximumTopSpacing)
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(topSpacing))

                    Column(
                        modifier = Modifier
                            .widthIn(max = HeartGuardComponentSize.AuthContentMaxWidth)
                            .fillMaxWidth()
                            .onSizeChanged { contentSize ->
                                if (formContentHeight != contentSize.height) {
                                    formContentHeight = contentSize.height
                                }
                            }
                            .padding(bottom = HeartGuardSpacing.Item),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
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
                            placeholder = stringResource(R.string.auth_login_password_hint),
                            isError = isPasswordError,
                            supportingText = passwordErrorMessage,
                            visualTransformation = PasswordVisualTransformation(),
                        )
                    }
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
                    isEnabled = !isSubmitting,
                )

                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(HeartGuardSpacing.Compact))
                    Text(
                        text = statusMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
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
            isPasswordError = true,
            passwordErrorMessage = stringResource(R.string.auth_password_error),
        )
    }
}
