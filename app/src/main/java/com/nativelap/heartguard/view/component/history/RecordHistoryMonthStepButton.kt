package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.extraColors

/** 날짜 선택 시트에서 달을 한 달씩 옮기는 버튼이다. [enabled]가 false면 누를 수 없고 흐리게 그린다. */
@Composable
internal fun RecordHistoryMonthStepButton(
    imageVector: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(HeartGuardComponentSize.TouchTarget)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint =
                if (enabled) {
                    MaterialTheme.extraColors.strongText
                } else {
                    MaterialTheme.extraColors.calendarDisabledText
                },
        )
    }
}
