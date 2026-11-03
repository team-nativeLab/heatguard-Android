package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize

/** 기록이 있는 날의 점이다. 선택한 날짜에서는 배경 위에 맞춰 onPrimary 색으로, 그 밖에서는 primary 색으로 그린다.
 * [hasRecord]가 false면 점을 그리지 않고 같은 자리만 비워 둔다. [dotSize]로 칸에 맞는 지름을 고른다. */
@Composable
internal fun RecordHistoryRecordDot(
    hasRecord: Boolean,
    isSelected: Boolean,
    dotSize: Dp = HeartGuardComponentSize.RecordDateDot,
    modifier: Modifier = Modifier,
) {
    val dotColor =
        if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.primary
        }
    val dotBackground =
        if (hasRecord) {
            dotColor
        } else {
            Color.Transparent
        }

    Box(
        modifier =
            modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(dotBackground),
    )
}
