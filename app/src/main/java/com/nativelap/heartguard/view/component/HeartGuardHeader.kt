package com.nativelap.heartguard.view.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardTheme

/** 좌우 액션 영역을 먼저 확보한 뒤 남은 폭에 제목을 배치하는 현장앱 헤더다. */
@Composable
fun HeartGuardHeader(
    title: String,
    menuPainter: Painter,
    notificationPainter: Painter,
    onMenuClick: () -> Unit,
    onNotificationClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasUnreadNotifications: Boolean = false,
) {
    HeartGuardHeaderBar(
        title = title,
        leadingAction = {
            HeartGuardHeaderActionButton(
                iconPainter = menuPainter,
                iconContentDescription = stringResource(R.string.common_menu),
                onClick = onMenuClick,
            )
        },
        notificationPainter = notificationPainter,
        onNotificationClick = onNotificationClick,
        hasUnreadNotifications = hasUnreadNotifications,
        modifier = modifier,
    )
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun HeartGuardHeaderPreview() {
    HeartGuardTheme {
        HeartGuardHeader(
            title = stringResource(R.string.brand_name),
            menuPainter = painterResource(R.drawable.menu_hamburger),
            notificationPainter = painterResource(R.drawable.notification_bell),
            onMenuClick = {},
            onNotificationClick = {},
        )
    }
}
