package com.nativelap.heartguard.view.route.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nativelap.heartguard.R
import com.nativelap.heartguard.view.screen.auth.AuthLoginScreen
import com.nativelap.heartguard.view.screen.auth.AuthSignUpScreen
import com.nativelap.heartguard.view.screen.auth.AuthSignUpScreenEvent
import com.nativelap.heartguard.viewmodel.auth.TeamLoginFailure
import com.nativelap.heartguard.viewmodel.auth.TeamLoginViewModel

/** 사전 발급된 작업자 계정의 로그인 요청과 화면 상태를 연결하는 Route이다. */
@Composable
internal fun HeartGuardLoginRoute(
    onNavigateToSignUp: () -> Unit = {},
    viewModel: TeamLoginViewModel = hiltViewModel(),
) {
    var email by rememberSaveable {
        mutableStateOf("")
    }
    var password by remember {
        mutableStateOf("")
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val statusMessage =
        when (uiState.failure) {
            null -> null
            TeamLoginFailure.INVALID_CREDENTIALS -> null
            TeamLoginFailure.ACCOUNT_DISABLED -> stringResource(R.string.auth_login_account_disabled)
            TeamLoginFailure.RATE_LIMITED -> stringResource(R.string.auth_login_rate_limited)
            TeamLoginFailure.GENERIC -> stringResource(R.string.auth_login_generic_error)
        }

    AuthLoginScreen(
        email = email,
        password = password,
        onEmailChange = { email = it },
        onPasswordChange = { password = it },
        onLoginClick = {
            viewModel.login(
                email = email.trim(),
                password = password,
            )
        },
        statusMessage = statusMessage,
        isPasswordError = uiState.failure == TeamLoginFailure.INVALID_CREDENTIALS,
        passwordErrorMessage =
            if (uiState.failure == TeamLoginFailure.INVALID_CREDENTIALS) {
                stringResource(R.string.auth_login_invalid_credentials)
            } else {
                null
            },
        isSubmitting = uiState.isSubmitting,
        onSignUpClick = onNavigateToSignUp,
    )
}

/** 회원가입 화면의 입력 상태를 화면 수명 안에서 관리한다. */
@Composable
internal fun HeartGuardSignUpRoute(onNavigateToLogin: () -> Unit) {
    var companyName by remember { mutableStateOf("") }
    var memberName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirmation by remember { mutableStateOf("") }

    AuthSignUpScreen(
        companyName = companyName,
        memberName = memberName,
        email = email,
        password = password,
        passwordConfirmation = passwordConfirmation,
        onEvent = { event ->
            when (event) {
                is AuthSignUpScreenEvent.CompanyNameChanged -> {
                    companyName = event.companyName
                }

                is AuthSignUpScreenEvent.MemberNameChanged -> {
                    memberName = event.memberName
                }

                is AuthSignUpScreenEvent.EmailChanged -> {
                    email = event.email
                }

                is AuthSignUpScreenEvent.PasswordChanged -> {
                    password = event.password
                }

                is AuthSignUpScreenEvent.PasswordConfirmationChanged -> {
                    passwordConfirmation = event.passwordConfirmation
                }

                AuthSignUpScreenEvent.LoginClicked -> {
                    onNavigateToLogin()
                }

                AuthSignUpScreenEvent.SignUpClicked -> {
                    Unit
                }
            }
        },
    )
}
