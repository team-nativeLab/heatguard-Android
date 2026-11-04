package com.nativelap.heartguard.view.route.menu

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.nativelap.heartguard.R
import com.nativelap.heartguard.viewmodel.menu.MenuDrawerViewModel

/** 진행 중 긴급 호출이 끝나지 않아 로그아웃이 막히면 안내한다. 메뉴 드로어를 띄우는 Route(현재 홈)마다 한 번 호출한다. */
@Composable
internal fun CollectMenuDrawerLogoutBlocked(menuDrawerViewModel: MenuDrawerViewModel) {
    val context = LocalContext.current
    val message = stringResource(R.string.menu_logout_blocked)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(menuDrawerViewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            menuDrawerViewModel.logoutBlocked.collect {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
