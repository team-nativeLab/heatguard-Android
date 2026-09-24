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

    // 비밀번호 확인란이 비어 있는 동안은(아직 입력을 시작하지 않았거나 지우는 중) 오류를 보류하고,
    // 값이 있는데 비밀번호와 다를 때만 오류로 표시한다. 두 값이 모두 비어 있으면 불일치는 아니지만
    // 가입 버튼도 활성화하지 않는다.
    val isPasswordMismatch = passwordConfirmation.isNotEmpty() && password != passwordConfirmation
    val isSignUpEnabled = password.isNotBlank() &&
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
