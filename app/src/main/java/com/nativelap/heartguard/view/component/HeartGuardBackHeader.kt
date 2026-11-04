package com.nativelap.heartguard.view.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 기록 흐름 화면용 헤더로, 왼쪽에 뒤로 가기 버튼을 둔다. */
@Composable
fun HeartGuardBackHeader(
    title: String,
    notificationPainter: Painter,
    onBackClick: () -> Unit,
    onNotificationClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasUnreadNotifications: Boolean = false,
) {
    HeartGuardHeaderBar(
        title = title,
        leadingAction = {
            val backDescription = stringResource(R.string.common_back_description)
            IconButton(
                onClick = onBackClick,
                modifier =
                    Modifier
                        .size(HeartGuardComponentSize.TouchTarget)
                        .semantics(mergeDescendants = true) {
                            contentDescription = backDescription
                        },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    modifier = Modifier.size(HeartGuardIconSize.HeaderAction),
                    tint = MaterialTheme.extraColors.strongText,
                )
            }
        },
        notificationPainter = notificationPainter,
        onNotificationClick = onNotificationClick,
        hasUnreadNotifications = hasUnreadNotifications,
        modifier = modifier,
    )
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun HeartGuardBackHeaderPreview() {
    HeartGuardTheme {
        HeartGuardBackHeader(
            title = stringResource(R.string.brand_name),
            notificationPainter = painterResource(R.drawable.notification_bell),
            onBackClick = {},
            onNotificationClick = {},
        )
    }
}
