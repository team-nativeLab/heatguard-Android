package com.nativelap.heartguard.view.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.auth.AuthPrimaryButton
import com.nativelap.heartguard.view.component.auth.AuthTextField
import com.nativelap.heartguard.view.component.auth.AuthTextFieldStyle
import com.nativelap.heartguard.view.component.auth.AuthTitleBlock
import com.nativelap.heartguard.view.component.brand.BrandMark

/** 이메일 로그인 화면을 기존 인증 Component 조합으로 구성한다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthLoginScreen(
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEmailError: Boolean = false,
    isPasswordError: Boolean = false,
    passwordErrorMessage: String? = null,
    statusMessage: String? = null,
    isSubmitting: Boolean = false,
    onSignUpClick: () -> Unit = {},
    titleResource: Int = R.string.auth_login_title,
    descriptionResource: Int = R.string.auth_login_description,
) {
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    // 이전 앱에서 열려 있던 키보드 inset이 진입 직후 잠깐 남아도 키보드 레이아웃으로 바뀌지 않도록
    // 로그인 입력칸에 포커스가 있을 때만 키보드가 열린 것으로 본다.
    var hasFormFocus by remember { mutableStateOf(false) }
    val isKeyboardVisible =
        hasFormFocus && WindowInsets.imeAnimationTarget.getBottom(LocalDensity.current) > 0
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
            // 키보드가 열리면 폼과 액션을 함께 스크롤해 큰 글꼴에서도 입력칸을 완전히 볼 수 있게 한다.
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoxWithConstraints(
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
                var formContentHeight by remember { mutableIntStateOf(0) }
                val topSpacing =
                    if (isKeyboardVisible || formContentHeight == 0) {
                        0.dp
                    } else {
                        with(LocalDensity.current) {
                            val maximumTopSpacing =
                                (HeartGuardSpacing.AuthTop - innerPadding.calculateTopPadding())
                                    .coerceAtLeast(0.dp)
                            (maxHeight - formContentHeight.toDp()).coerceIn(0.dp, maximumTopSpacing)
                        }
                    }
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
                    Spacer(modifier = Modifier.height(topSpacing))

                    Column(
                        modifier =
                            Modifier
                                .widthIn(max = HeartGuardComponentSize.PageContentMaxWidth)
                                .fillMaxWidth()
                                .onSizeChanged { contentSize ->
                                    if (formContentHeight != contentSize.height) {
                                        formContentHeight = contentSize.height
                                    }
                                }.padding(bottom = HeartGuardSpacing.Item),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        BrandMark(
                            brandPainter = painterResource(R.drawable.heart_guard_logo),
                            contentDescription = stringResource(R.string.brand_name),
                        )

                        Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthLogoTitle))

                        AuthTitleBlock(
                            title = stringResource(titleResource),
                            description = stringResource(descriptionResource),
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthTitleForm))

                        AuthTextField(
                            label = stringResource(R.string.auth_email),
                            text = email,
                            onTextChange = onEmailChange,
                            placeholder = stringResource(R.string.auth_email_hint),
                            isError = isEmailError,
                            errorSemanticsMessage = passwordErrorMessage.takeIf { isEmailError },
                            style = AuthTextFieldStyle.SoftFilled,
                        )

                        Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthFieldGroup))

                        AuthTextField(
                            label = stringResource(R.string.auth_password),
                            text = password,
                            onTextChange = onPasswordChange,
                            placeholder = stringResource(R.string.auth_login_password_hint),
                            isError = isPasswordError,
                            supportingText = passwordErrorMessage,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation =
                                if (isPasswordVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector =
                                            if (isPasswordVisible) {
                                                Icons.Outlined.VisibilityOff
                                            } else {
                                                Icons.Outlined.Visibility
                                            },
                                        contentDescription =
                                            stringResource(
                                                if (isPasswordVisible) {
                                                    R.string.password_hide
                                                } else {
                                                    R.string.password_show
                                                },
                                            ),
                                        tint = MaterialTheme.extraColors.authOnSurfaceVariant,
                                    )
                                }
                            },
                            style = AuthTextFieldStyle.SoftFilled,
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
                        ).padding(bottom = HeartGuardSpacing.AuthActionBottom)
                        .widthIn(max = HeartGuardComponentSize.AuthActionMaxWidth)
                        .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(HeartGuardSpacing.Compact))

                    Text(
                        text = statusMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )

                    Spacer(modifier = Modifier.height(HeartGuardSpacing.Compact))
                }

                AuthPrimaryButton(
                    title = stringResource(R.string.auth_login),
                    onClick = onLoginClick,
                    modifier = Modifier.heightIn(min = HeartGuardComponentSize.TouchTarget),
                    isEnabled = !isSubmitting,
                )

                TextButton(
                    onClick = onSignUpClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.auth_signup_prompt),
                            color = MaterialTheme.extraColors.authOnSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(R.string.auth_signup_link),
                            modifier = Modifier.padding(start = HeartGuardSpacing.AuthTitleDescription),
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
            isEmailError = true,
            isPasswordError = true,
            passwordErrorMessage = stringResource(R.string.auth_password_error),
        )
    }
}
