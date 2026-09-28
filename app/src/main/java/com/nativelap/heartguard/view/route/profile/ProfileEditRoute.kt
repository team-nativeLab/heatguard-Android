package com.nativelap.heartguard.view.route.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.view.screen.profile.ProfileEditScreen
import com.nativelap.heartguard.viewmodel.profile.ProfileEditScreenEvent

/** 작업자 프로필 API가 제공되지 않는 동안 읽기/저장을 막은 프로필 화면을 표시한다. */
@Composable
internal fun HeartGuardProfileEditRoute(
    onBackClick: () -> Unit,
) {
    val unavailableValue = stringResource(R.string.common_empty_value)
    ProfileEditScreen(
        companyName = unavailableValue,
        userName = unavailableValue,
        email = unavailableValue,
        onEvent = { event ->
            when (event) {
                ProfileEditScreenEvent.BackClicked -> onBackClick()
            }
        },
    )
}
