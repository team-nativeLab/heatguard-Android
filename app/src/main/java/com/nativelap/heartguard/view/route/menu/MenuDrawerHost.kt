package com.nativelap.heartguard.view.route.menu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nativelap.heartguard.view.component.menu.MenuDrawerContent
import com.nativelap.heartguard.view.component.menu.MenuDrawerOverlay
import com.nativelap.heartguard.viewmodel.menu.MenuDrawerEvent
import com.nativelap.heartguard.viewmodel.menu.MenuDrawerViewModel

/** 기록·사진 화면 헤더의 메뉴와 알림 동작을 연결한다. */
@Composable
internal fun MenuDrawerHost(
    onProfileEditClick: () -> Unit,
    onInquiryClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    menuDrawerViewModel: MenuDrawerViewModel = hiltViewModel(),
    content: @Composable (onMenuClick: () -> Unit, onNotificationClick: () -> Unit) -> Unit,
) {
    val menuDrawerProfile by menuDrawerViewModel.profile.collectAsStateWithLifecycle()
    var isMenuDrawerOpen by rememberSaveable {
        mutableStateOf(false)
    }

    BackHandler(enabled = isMenuDrawerOpen) {
        isMenuDrawerOpen = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content(
            {
                isMenuDrawerOpen = true
                menuDrawerViewModel.loadProfile()
            },
            onNotificationsClick,
        )

        MenuDrawerOverlay(
            isVisible = isMenuDrawerOpen,
            onDismissRequest = {
                isMenuDrawerOpen = false
            },
        ) {
            MenuDrawerContent(
                profile = menuDrawerProfile,
                onEvent = { event ->
                    isMenuDrawerOpen = false
                    when (event) {
                        MenuDrawerEvent.EditProfileClicked -> onProfileEditClick()
                        MenuDrawerEvent.InquiryClicked -> onInquiryClick()
                        MenuDrawerEvent.LogoutClicked -> menuDrawerViewModel.logout()
                        MenuDrawerEvent.WithdrawClicked -> onWithdrawClick()
                    }
                },
            )
        }

    }
}
