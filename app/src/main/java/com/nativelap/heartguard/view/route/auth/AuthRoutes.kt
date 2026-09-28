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
import com.nativelap.heartguard.viewmodel.auth.TeamLoginFailure
import com.nativelap.heartguard.viewmodel.auth.TeamLoginViewModel

/** 사전 발급된 작업자 계정의 로그인 요청과 화면 상태를 연결하는 Route이다. */
@Composable
internal fun HeartGuardLoginRoute(
    viewModel: TeamLoginViewModel = hiltViewModel(),
) {
    var email by rememberSaveable {
        mutableStateOf("")
    }
    var password by remember {
        mutableStateOf("")
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val statusMessage = when (uiState.failure) {
        null -> null
        TeamLoginFailure.INVALID_CREDENTIALS -> stringResource(R.string.auth_login_invalid_credentials)
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
        isSubmitting = uiState.isSubmitting,
    )
}
