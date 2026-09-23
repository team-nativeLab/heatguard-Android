package com.nativelap.heartguard.view.route.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.view.screen.auth.AuthLoginScreen
import com.nativelap.heartguard.view.screen.auth.AuthSignUpScreen

/** 로그인 입력 상태를 보유하고 로그인 Screen에 Navigation callback을 전달하는 Route이다. */
@Composable
internal fun HeartGuardLoginRoute() {
    var email by rememberSaveable {
        mutableStateOf("")
    }
    var password by rememberSaveable {
        mutableStateOf("")
    }
    var isLoginContractPending by rememberSaveable {
        mutableStateOf(false)
    }

    AuthLoginScreen(
        email = email,
        password = password,
        onEmailChange = { email = it },
        onPasswordChange = { password = it },
        onLoginClick = { isLoginContractPending = true },
        loginInfoMessage = if (isLoginContractPending) {
            stringResource(R.string.auth_login_contract_pending)
        } else {
            null
        },
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
    )
}
