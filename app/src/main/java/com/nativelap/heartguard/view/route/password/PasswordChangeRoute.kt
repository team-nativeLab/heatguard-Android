package com.nativelap.heartguard.view.route.password

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nativelap.heartguard.R
import com.nativelap.heartguard.view.screen.password.PasswordChangeScreen
import com.nativelap.heartguard.viewmodel.password.PasswordChangeEffect
import com.nativelap.heartguard.viewmodel.password.PasswordChangeScreenEvent
import com.nativelap.heartguard.viewmodel.password.PasswordChangeViewModel

/** 비밀번호 변경 화면이다. 변경에 성공하면 "변경 완료"를 알리고 내 정보 수정 화면으로 돌아간다(Figma 흐름 26→24).
 * 서버는 변경 후에도 현재 세션을 유지하므로 다시 로그인시키지 않는다(2026-09-28 실서버 확인). */
@Composable
internal fun HeartGuardPasswordChangeRoute(
    onBackClick: () -> Unit,
    viewModel: PasswordChangeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val changedMessage = stringResource(R.string.password_change_done)
    val currentOnBackClick by rememberUpdatedState(onBackClick)

    LaunchedEffect(viewModel) {
        viewModel.openScreen()
    }

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { passwordChangeEffect ->
                when (passwordChangeEffect) {
                    PasswordChangeEffect.Changed -> {
                        Toast
                            .makeText(context, changedMessage, Toast.LENGTH_SHORT)
                            .show()
                        currentOnBackClick()
                    }
                }
            }
        }
    }

    PasswordChangeScreen(
        uiState = uiState,
        onEvent = { event ->
            when (event) {
                PasswordChangeScreenEvent.BackClicked -> onBackClick()
                is PasswordChangeScreenEvent.CurrentPasswordChanged -> viewModel.updateCurrentPassword(event.password)
                is PasswordChangeScreenEvent.NewPasswordChanged -> viewModel.updateNewPassword(event.password)
                is PasswordChangeScreenEvent.ConfirmPasswordChanged -> viewModel.updateConfirmPassword(event.password)
                PasswordChangeScreenEvent.CurrentPasswordVisibilityClicked -> viewModel.toggleCurrentPasswordVisibility()
                PasswordChangeScreenEvent.NewPasswordVisibilityClicked -> viewModel.toggleNewPasswordVisibility()
                PasswordChangeScreenEvent.ConfirmPasswordVisibilityClicked -> viewModel.toggleConfirmPasswordVisibility()
                PasswordChangeScreenEvent.SubmitClicked -> viewModel.submit()
            }
        },
    )
}
