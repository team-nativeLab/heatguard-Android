package com.nativelap.heartguard.view.route.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.nativelap.heartguard.view.screen.auth.AuthLoginScreen
import com.nativelap.heartguard.view.screen.auth.AuthSignUpScreen

/** 로그인 입력 상태를 보유하고 로그인 Screen에 Navigation callback을 전달하는 Route이다. */
@Composable
internal fun HeartGuardLoginRoute(
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit,
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
        onSignUpClick = onSignUpClick,
    )
}

/** 회원가입 입력 상태를 보유하고 회원가입 Screen에 Navigation callback을 전달하는 Route이다. */
@Composable
internal fun HeartGuardSignUpRoute(
    onSignUpClick: () -> Unit,
    onLoginClick: () -> Unit,
) {
    var companyName by rememberSaveable {
        mutableStateOf("")
    }
    var name by rememberSaveable {
        mutableStateOf("")
    }
    var email by rememberSaveable {
        mutableStateOf("")
    }
    var password by rememberSaveable {
        mutableStateOf("")
    }
    var passwordConfirmation by rememberSaveable {
        mutableStateOf("")
    }

    // 비밀번호 확인란이 비밀번호보다 짧은 동안은(아직 입력 중일 가능성이 높다) 오류를 보류하고,
    // 최소한 비밀번호만큼 입력했는데도 다를 때만 오류로 표시한다 — 그렇지 않으면 사용자가 입력을
    // 끝내기도 전에 매 글자마다 오류가 깜빡인다.
    val isPasswordMismatch = passwordConfirmation.isNotEmpty() &&
        passwordConfirmation.length >= password.length &&
        password != passwordConfirmation
    // 비밀번호 일치뿐 아니라 회사명·이름·이메일도 비어 있지 않아야 가입 버튼을 활성화한다.
    val isSignUpEnabled = companyName.isNotBlank() &&
        name.isNotBlank() &&
        email.isNotBlank() &&
        password.isNotBlank() &&
        passwordConfirmation.isNotBlank() &&
        password == passwordConfirmation

    AuthSignUpScreen(
        companyName = companyName,
        name = name,
        email = email,
        password = password,
        passwordConfirmation = passwordConfirmation,
        onCompanyNameChange = { companyName = it },
        onNameChange = { name = it },
        onEmailChange = { email = it },
        onPasswordChange = { password = it },
        onPasswordConfirmationChange = { passwordConfirmation = it },
        onSignUpClick = onSignUpClick,
        onLoginClick = onLoginClick,
        isSignUpEnabled = isSignUpEnabled,
        isPasswordMismatch = isPasswordMismatch,
    )
}
