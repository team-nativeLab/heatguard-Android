package com.nativelap.heartguard.view.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme

/** 가운데 제목과 좌우 액션을 독립적으로 배치하는 현장앱 헤더다. */
@Composable
fun HeartGuardHeader(
    title: String,
    menuPainter: Painter,
    notificationPainter: Painter,
    onMenuClick: () -> Unit,
    onNotificationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HeartGuardComponentSize.TouchTarget)
            .padding(horizontal = HeartGuardSpacing.HeaderHorizontal),
        contentAlignment = Alignment.Center,
    ) {
        HeartGuardHeaderActionButton(
            iconPainter = menuPainter,
            iconContentDescription = stringResource(R.string.common_menu),
            onClick = onMenuClick,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = HeartGuardFontSize.PageTitle,
                lineHeight = HeartGuardFontSize.PageTitle,
            ),
        )
        HeartGuardHeaderActionButton(
            iconPainter = notificationPainter,
            iconContentDescription = stringResource(R.string.common_notifications),
            onClick = onNotificationClick,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
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
