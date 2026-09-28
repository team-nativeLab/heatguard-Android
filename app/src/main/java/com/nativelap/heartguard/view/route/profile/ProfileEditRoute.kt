package com.nativelap.heartguard.view.route.profile

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
import com.nativelap.heartguard.view.screen.profile.ProfileEditScreen
import com.nativelap.heartguard.viewmodel.profile.ProfileEditEffect
import com.nativelap.heartguard.viewmodel.profile.ProfileEditScreenEvent
import com.nativelap.heartguard.viewmodel.profile.ProfileEditViewModel

/** 내 정보 수정 화면에 들어올 때마다 작업자 정보를 새로 조회하고, 이름 저장이 끝나면 "저장 완료"를 알린 뒤 이전 화면으로 돌아간다. */
@Composable
internal fun HeartGuardProfileEditRoute(
    onBackClick: () -> Unit,
    viewModel: ProfileEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val savedMessage = stringResource(R.string.profile_saved)
    val currentOnBackClick by rememberUpdatedState(onBackClick)

    LaunchedEffect(viewModel) {
        viewModel.loadProfile()
    }

    // 저장 완료는 한 번만 처리해야 하는 결과라 화면이 보이는 동안에만 수집한다.
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { profileEditEffect ->
                when (profileEditEffect) {
                    ProfileEditEffect.Saved -> {
                        Toast
                            .makeText(context, savedMessage, Toast.LENGTH_SHORT)
                            .show()
                        currentOnBackClick()
                    }
                }
            }
        }
    }

    ProfileEditScreen(
        uiState = uiState,
        onEvent = { event ->
            when (event) {
                ProfileEditScreenEvent.BackClicked -> onBackClick()
                is ProfileEditScreenEvent.NameChanged -> viewModel.updateName(event.name)
                ProfileEditScreenEvent.SaveClicked -> viewModel.saveProfile()
                ProfileEditScreenEvent.RetryClicked -> viewModel.loadProfile()
            }
        },
    )
}
