package com.nativelap.heartguard.view.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

/** Figma 회원가입 화면에 맞춰 작업자 등록 입력 UI를 표시한다. */
@Composable
internal fun AuthSignUpScreen(
    companyName: String,
    memberName: String,
    email: String,
    password: String,
    passwordConfirmation: String,
    onEvent: (AuthSignUpScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .verticalScroll(formScrollState)
                    .padding(horizontal = HeartGuardSpacing.AuthHorizontal),
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
                    label = stringResource(R.string.auth_signup_company_label),
                    text = companyName,
                    onTextChange = { value ->
                        onEvent(AuthSignUpScreenEvent.CompanyNameChanged(value))
                    },
                    placeholder = stringResource(R.string.auth_signup_company_hint),
                )

                Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpFieldGroup))

                AuthTextField(
                    label = stringResource(R.string.auth_signup_member_label),
                    text = memberName,
                    onTextChange = { value ->
                        onEvent(AuthSignUpScreenEvent.MemberNameChanged(value))
                    },
                    placeholder = stringResource(R.string.auth_signup_member_hint),
                )

                Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpFieldGroup))

                AuthTextField(
                    label = stringResource(R.string.auth_email),
                    text = email,
                    onTextChange = { value ->
                        onEvent(AuthSignUpScreenEvent.EmailChanged(value))
                    },
                    placeholder = stringResource(R.string.auth_email_hint),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )

                Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpFieldGroup))

                AuthTextField(
                    label = stringResource(R.string.auth_password),
                    text = password,
                    onTextChange = { value ->
                        onEvent(AuthSignUpScreenEvent.PasswordChanged(value))
                    },
                    placeholder = stringResource(R.string.auth_signup_password_hint),
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                )

                Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthSignUpActionsTop))

                AuthPrimaryButton(
                    title = stringResource(R.string.auth_signup),
                    onClick = {
                        onEvent(AuthSignUpScreenEvent.SignUpClicked)
                    },
                )

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

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun AuthSignUpScreenPreview() {
    HeartGuardTheme {
        AuthSignUpScreen(
            companyName = "",
            memberName = "",
            email = "",
            password = "",
            passwordConfirmation = "",
            onEvent = {},
        )
    }
}
