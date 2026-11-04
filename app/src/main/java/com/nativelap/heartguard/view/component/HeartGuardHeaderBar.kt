package com.nativelap.heartguard.view.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing

/** 왼쪽 액션, 가운데 제목, 오른쪽 알림 버튼을 같은 배치로 묶는 헤더 공용 구조다. */
@Composable
internal fun HeartGuardHeaderBar(
    title: String,
    leadingAction: @Composable () -> Unit,
    notificationPainter: Painter,
    onNotificationClick: () -> Unit,
    hasUnreadNotifications: Boolean,
    modifier: Modifier = Modifier,
) {
    // 이 Row는 메뉴, 알림 벨, 뒤로 가기 chevron이 함께 쓰는 헤더 공용 구조다.
    // 접근성을 위해 아이콘 버튼은 48dp 터치 타깃(HeartGuardComponentSize.TouchTarget)을 유지하고,
    // 아이콘은 그 안에서 28dp(HeartGuardIconSize.HeaderAction)로 중앙 배치한다.
    // 터치 타깃에 Figma 여백(HeaderHorizontal=24dp)을 그대로 주면 아이콘이 (터치 타깃 - 아이콘 크기)/2만큼 더 안쪽으로 밀리므로,
    // 그 차이만큼 Row 패딩을 줄여 의도한 위치를 맞추도록 했다.
    // 뒤로 가기 chevron은 Material 아이콘 내부 여백 때문에 메뉴 PNG와 시각적 왼쪽 끝이 다를 수 있다.
    // 기기에서 정렬을 확인하기 전이므로 정렬 일치는 확인 필요 상태다.
    val headerActionTouchInset = (HeartGuardComponentSize.TouchTarget - HeartGuardIconSize.HeaderAction) / 2
    val headerHorizontalPadding = HeartGuardSpacing.HeaderHorizontal - headerActionTouchInset

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = HeartGuardComponentSize.TouchTarget)
                .padding(horizontal = headerHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingAction()

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = HeartGuardSpacing.Compact),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = HeartGuardFontSize.PageTitle,
                        lineHeight = HeartGuardFontSize.PageTitle,
                    ),
            )
        }

        Box(
            modifier = Modifier.size(HeartGuardComponentSize.TouchTarget),
            contentAlignment = Alignment.Center,
        ) {
            HeartGuardHeaderActionButton(
                iconPainter = notificationPainter,
                iconContentDescription =
                    if (hasUnreadNotifications) {
                        stringResource(R.string.common_notifications_unread)
                    } else {
                        stringResource(R.string.common_notifications)
                    },
                onClick = onNotificationClick,
            )

            if (hasUnreadNotifications) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(
                                x = -HeartGuardUnreadIndicatorEdgeOffset,
                                y = HeartGuardUnreadIndicatorEdgeOffset,
                            ).size(HeartGuardUnreadIndicatorSize)
                            .background(MaterialTheme.colorScheme.error, CircleShape),
                )
            }
        }
    }
}

private val HeartGuardUnreadIndicatorSize = 8.dp
private val HeartGuardUnreadIndicatorEdgeOffset = 6.dp
