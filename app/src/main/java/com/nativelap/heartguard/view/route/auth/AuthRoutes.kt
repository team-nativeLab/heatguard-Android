package com.nativelap.heartguard.view.route.auth

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nativelap.heartguard.R
import com.nativelap.heartguard.core.session.PendingAppLink
import com.nativelap.heartguard.view.screen.auth.AuthLoginScreen
import com.nativelap.heartguard.view.screen.auth.AuthPhoneNumberScreen
import com.nativelap.heartguard.view.screen.auth.AuthSignUpScreen
import com.nativelap.heartguard.view.screen.auth.AuthSignUpScreenEvent
import com.nativelap.heartguard.viewmodel.auth.AppAccountLoginViewModel
import com.nativelap.heartguard.viewmodel.auth.AppAccountSignUpFailure
import com.nativelap.heartguard.viewmodel.auth.AppAccountSignUpViewModel
import com.nativelap.heartguard.viewmodel.auth.TeamLoginFailure
import com.nativelap.heartguard.viewmodel.auth.TeamLoginViewModel
import com.nativelap.heartguard.viewmodel.site.ClaimAppSiteLinkFailure
import com.nativelap.heartguard.viewmodel.site.ClaimAppSiteLinkViewModel

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

@Composable
internal fun HeartGuardAppAccountLoginRoute(
    onNavigateToSignUp: () -> Unit,
    viewModel: AppAccountLoginViewModel = hiltViewModel(),
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AuthLoginScreen(
        email = email,
        password = password,
        onEmailChange = { email = it },
        onPasswordChange = { password = it },
        onLoginClick = { viewModel.login(email = email, password = password) },
        titleResource = R.string.auth_login_app_account,
        descriptionResource = R.string.auth_login_app_account_description,
        isSubmitting = uiState.isSubmitting,
        statusMessage = if (uiState.hasFailure) stringResource(R.string.auth_login_generic_error) else null,
        onSignUpClick = onNavigateToSignUp,
    )
}

@Composable
internal fun HeartGuardPhoneNumberRoute(
    pendingAppLink: PendingAppLink,
    onClaimSuccess: (PendingAppLink) -> Unit,
    onCancelClaim: (PendingAppLink) -> Unit,
    onSessionRequired: () -> Unit,
    isAppAccountSession: Boolean,
    viewModel: ClaimAppSiteLinkViewModel = hiltViewModel(),
) {
    var phoneNumber by remember(pendingAppLink.linkId) { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(
        enabled = uiState.linkId == pendingAppLink.linkId,
    ) {
        if (!uiState.isSubmitting) {
            onCancelClaim(pendingAppLink)
        }
    }
    val statusMessage =
        when (uiState.failure.takeIf { uiState.linkId == pendingAppLink.linkId }) {
            null -> null
            ClaimAppSiteLinkFailure.PHONE_MISMATCH -> stringResource(R.string.auth_phone_mismatch_error)
            ClaimAppSiteLinkFailure.LINK_UNAVAILABLE -> stringResource(R.string.auth_phone_link_unavailable_error)
            ClaimAppSiteLinkFailure.RATE_LIMITED -> stringResource(R.string.auth_phone_rate_limited_error)
            ClaimAppSiteLinkFailure.NETWORK -> stringResource(R.string.auth_phone_network_error)
            ClaimAppSiteLinkFailure.SESSION_REQUIRED -> stringResource(R.string.auth_phone_session_required_error)
            ClaimAppSiteLinkFailure.GENERIC -> stringResource(R.string.auth_phone_generic_error)
        }

    LaunchedEffect(pendingAppLink.linkId, isAppAccountSession) {
        viewModel.activateLink(
            pendingAppLink = pendingAppLink,
            clearSessionRequiredFailure = isAppAccountSession,
        )
    }

    LaunchedEffect(uiState.linkId, uiState.completedLinkId) {
        if (uiState.linkId == pendingAppLink.linkId &&
            uiState.completedLinkId == pendingAppLink.linkId
        ) {
            onClaimSuccess(pendingAppLink)
        }
    }

    LaunchedEffect(uiState.linkId, uiState.failure) {
        if (uiState.linkId == pendingAppLink.linkId &&
            uiState.failure == ClaimAppSiteLinkFailure.SESSION_REQUIRED
        ) {
            onSessionRequired()
        }
    }

    AuthPhoneNumberScreen(
        phoneNumber = phoneNumber,
        onPhoneNumberChange = { enteredPhoneNumber ->
            phoneNumber = enteredPhoneNumber.filter { digit -> digit in '0'..'9' }
        },
        onLinkClick = {
            viewModel.claim(
                pendingAppLink = pendingAppLink,
                managerPhone = phoneNumber,
            )
        },
        isSubmitting = uiState.linkId != pendingAppLink.linkId || uiState.isSubmitting,
        statusMessage = statusMessage,
    )
}

/** 회원가입 화면의 입력 상태를 화면 수명 안에서 관리한다. */
@Composable
internal fun HeartGuardSignUpRoute(
    onNavigateToLogin: () -> Unit,
    viewModel: AppAccountSignUpViewModel = hiltViewModel(),
) {
    var email by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirmation by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isCodeSentForCurrentEmail = uiState.emailForRequestedCode == email.trim()
    val statusMessage =
        when (uiState.failure) {
            null -> {
                if (isCodeSentForCurrentEmail) {
                    stringResource(R.string.auth_signup_code_sent)
                } else {
                    null
                }
            }

            AppAccountSignUpFailure.EMAIL_CODE_REQUIRED -> {
                stringResource(R.string.auth_signup_code_required)
            }

            AppAccountSignUpFailure.EMAIL_CODE_REQUEST -> {
                stringResource(R.string.auth_signup_email_code_error)
            }

            AppAccountSignUpFailure.EMAIL_ALREADY_EXISTS -> {
                stringResource(R.string.auth_signup_email_exists_error)
            }

            AppAccountSignUpFailure.RATE_LIMITED -> {
                stringResource(R.string.auth_signup_rate_limited_error)
            }

            AppAccountSignUpFailure.NETWORK -> {
                stringResource(R.string.auth_signup_network_error)
            }

            AppAccountSignUpFailure.INVALID_FORM -> {
                stringResource(R.string.auth_signup_form_error)
            }

            AppAccountSignUpFailure.INVALID_VERIFICATION_CODE -> {
                stringResource(R.string.auth_signup_invalid_code_error)
            }

            AppAccountSignUpFailure.ACCOUNT_NOT_READY -> {
                stringResource(R.string.auth_signup_account_not_ready_error)
            }

            AppAccountSignUpFailure.REGISTRATION -> {
                stringResource(R.string.auth_signup_registration_error)
            }

            AppAccountSignUpFailure.LOGIN_AFTER_REGISTRATION -> {
                stringResource(R.string.auth_signup_login_after_register_error)
            }
        }

    AuthSignUpScreen(
        email = email,
        verificationCode = verificationCode,
        password = password,
        passwordConfirmation = passwordConfirmation,
        isSendingEmailCode = uiState.isRequestingEmailCode,
        isEmailCodeSent = isCodeSentForCurrentEmail,
        resendCooldownSeconds = uiState.resendCooldownSeconds,
        isSubmitting = uiState.isSubmitting,
        statusMessage = statusMessage,
        onEvent = { event ->
            when (event) {
                is AuthSignUpScreenEvent.EmailChanged -> {
                    email = event.email
                }

                is AuthSignUpScreenEvent.VerificationCodeChanged -> {
                    verificationCode = event.verificationCode
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
                    viewModel.signUp(
                        email = email,
                        password = password,
                        confirmPassword = passwordConfirmation,
                        verificationCode = verificationCode,
                    )
                }

                AuthSignUpScreenEvent.RequestEmailCodeClicked -> {
                    viewModel.requestEmailCode(email)
                }
            }
        },
    )
}
