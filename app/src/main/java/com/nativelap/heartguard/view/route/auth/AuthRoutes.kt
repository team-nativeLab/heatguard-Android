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
import java.util.Locale

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
        isEmailError = uiState.failure == TeamLoginFailure.INVALID_CREDENTIALS,
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
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirmation by remember { mutableStateOf("") }
    var step by rememberSaveable { mutableStateOf(0) }
    var hasRequestedCodeFromEmailStep by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val normalizedEmail = email.trim().lowercase(Locale.ROOT)
    val isCodeSentForCurrentEmail = uiState.emailForRequestedCode == normalizedEmail

    BackHandler(
        enabled = uiState.isSubmitting || uiState.isRequestingEmailCode || uiState.isVerifyingEmailCode || step > 0,
    ) {
        if (!uiState.isSubmitting && !uiState.isRequestingEmailCode && !uiState.isVerifyingEmailCode) {
            viewModel.clearEmailCodeVerification()
            step = (step - 1).coerceAtLeast(0)
            hasRequestedCodeFromEmailStep = false
        }
    }
    LaunchedEffect(uiState.isRequestingEmailCode, uiState.failure, uiState.emailForRequestedCode) {
        if (hasRequestedCodeFromEmailStep && !uiState.isRequestingEmailCode && uiState.failure == null &&
            uiState.emailForRequestedCode?.isNotBlank() == true && isCodeSentForCurrentEmail && step == 0
        ) {
            step = 1
            hasRequestedCodeFromEmailStep = false
        } else if (uiState.failure != null) {
            hasRequestedCodeFromEmailStep = false
        }
    }
    LaunchedEffect(uiState.failure) {
        if (uiState.failure == AppAccountSignUpFailure.INVALID_VERIFICATION_CODE) {
            step = 1
        }
    }
    LaunchedEffect(uiState.verifiedEmail, uiState.isVerifyingEmailCode) {
        if (uiState.verifiedEmail == normalizedEmail && !uiState.isVerifyingEmailCode && step == 1) {
            step = 2
        }
    }
    LaunchedEffect(step, email, uiState.verifiedEmail, uiState.failure) {
        if (step == 2 &&
            uiState.verifiedEmail != normalizedEmail &&
            uiState.failure != AppAccountSignUpFailure.INVALID_VERIFICATION_CODE
        ) {
            step = 0
        }
    }
    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onNavigateToLogin()
        }
    }
    val fallbackStatusMessage =
        when (uiState.failure) {
            null -> {
                if (uiState.isVerifyingEmailCode) {
                    stringResource(R.string.auth_signup_code_verifying)
                } else if (step <= 1 && isCodeSentForCurrentEmail) {
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

            AppAccountSignUpFailure.EMAIL_CODE_VERIFICATION -> {
                stringResource(R.string.auth_signup_code_verification_error)
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
        }
    val statusMessage =
        uiState.failureMessage?.takeIf { serverMessage -> serverMessage.isNotBlank() }
            ?: fallbackStatusMessage

    AuthSignUpScreen(
        name = name,
        email = email,
        verificationCode = verificationCode,
        password = password,
        passwordConfirmation = passwordConfirmation,
        isSendingEmailCode = uiState.isRequestingEmailCode,
        isEmailCodeSent = isCodeSentForCurrentEmail,
        isEmailCodeResendBlocked =
            (uiState.emailCodeCooldownSecondsByEmail[normalizedEmail] ?: 0) > 0,
        emailCodeRemainingSeconds = uiState.emailCodeRemainingSecondsByEmail[normalizedEmail] ?: 0,
        isSubmitting = uiState.isSubmitting || uiState.isVerifyingEmailCode,
        statusMessage = statusMessage,
        isStatusError = uiState.failure != null,
        step = step,
        onEvent = { event ->
            when (event) {
                is AuthSignUpScreenEvent.NameChanged -> {
                    name = event.name
                }

                is AuthSignUpScreenEvent.EmailChanged -> {
                    if (email.trim().lowercase(Locale.ROOT) != event.email.trim().lowercase(Locale.ROOT)) {
                        viewModel.clearEmailCodeVerification()
                        verificationCode = ""
                        step = 0
                        hasRequestedCodeFromEmailStep = false
                    }
                    email = event.email
                }

                is AuthSignUpScreenEvent.VerificationCodeChanged -> {
                    verificationCode = event.verificationCode.filter { digit -> digit in '0'..'9' }.take(6)
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

                AuthSignUpScreenEvent.ContinueClicked -> {
                    if (step == 1) {
                        viewModel.verifyEmailCode(email, verificationCode)
                    } else {
                        step = if (step == 0) 1 else 2
                    }
                    hasRequestedCodeFromEmailStep = false
                }

                AuthSignUpScreenEvent.StepBackClicked -> {
                    viewModel.clearEmailCodeVerification()
                    step = (step - 1).coerceAtLeast(0)
                    hasRequestedCodeFromEmailStep = false
                }

                AuthSignUpScreenEvent.SignUpClicked -> {
                    viewModel.signUp(
                        email = email,
                        password = password,
                        confirmPassword = passwordConfirmation,
                    )
                }

                AuthSignUpScreenEvent.RequestEmailCodeClicked -> {
                    if (step == 0) {
                        hasRequestedCodeFromEmailStep = true
                    } else if (step == 1) {
                        verificationCode = ""
                    }
                    viewModel.requestEmailCode(email)
                }
            }
        },
    )
}
