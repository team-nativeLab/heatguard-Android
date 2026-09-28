package com.nativelap.heartguard.view.route.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.nativelap.heartguard.view.screen.auth.AuthLoginScreen

/** 로그인 입력 상태를 보유하고 로그인 Screen에 Navigation callback을 전달하는 Route이다. */
@Composable
internal fun HeartGuardLoginRoute(
    onLoginClick: () -> Unit,
) {
    var email by rememberSaveable {
        mutableStateOf("")
    }
    var password by rememberSaveable {
        mutableStateOf("")
    }

    AuthLoginScreen(
        email = email,
        password = password,
        onEmailChange = { email = it },
        onPasswordChange = { password = it },
        onLoginClick = onLoginClick,
    )
}
