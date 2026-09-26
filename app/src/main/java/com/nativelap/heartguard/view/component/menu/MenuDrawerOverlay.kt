package com.nativelap.heartguard.view.component.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.extraColors

/** 화면 위에 딤을 깔고 왼쪽에서 드로어 패널을 밀어 넣는 오버레이다.
 * 열림 상태는 호출한 Route가 소유하며, 딤을 누르면 [onDismissRequest]로 닫기를 요청한다.
 * 앱이 edge-to-edge라 패널 안쪽에 시스템 바 inset을 직접 적용한다. */
@Composable
fun MenuDrawerOverlay(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val closeDescription = stringResource(R.string.menu_close_description)
    val paneDescription = stringResource(R.string.common_menu)

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.extraColors.overlayScrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = closeDescription,
                        role = Role.Button,
                        onClick = onDismissRequest,
                    ),
            )
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally { fullWidth -> -fullWidth },
            exit = slideOutHorizontally { fullWidth -> -fullWidth },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(HeartGuardComponentSize.MenuDrawerWidth)
                    .background(MaterialTheme.colorScheme.surface)
                    .semantics {
                        paneTitle = paneDescription
                    }
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(
                        horizontal = HeartGuardSpacing.MenuDrawerHorizontal,
                        vertical = HeartGuardSpacing.MenuDrawerVertical,
                    ),
            ) {
                content()
            }
        }
    }
}
