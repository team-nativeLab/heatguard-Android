package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardBorderWidth
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 기록 유형 필터 칩 하나다. 선택 상태를 색과 접근성 selected 정보로 함께 알린다. */
@Composable
fun RecordHistoryFilterChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.semantics {
            role = Role.Tab
            selected = isSelected
        },
        shape = RoundedCornerShape(HeartGuardRadius.Pill),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = if (isSelected) {
            null
        } else {
            BorderStroke(
                width = HeartGuardBorderWidth.Divider,
                color = MaterialTheme.extraColors.cardBorder,
            )
        },
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(
                horizontal = HeartGuardSpacing.Item,
                vertical = HeartGuardSpacing.Compact,
            ),
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.extraColors.secondaryText
            },
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Preview
@Composable
private fun RecordHistoryFilterChipPreview() {
    HeartGuardTheme {
        RecordHistoryFilterChip(
            title = "전체",
            isSelected = true,
            onClick = {},
        )
    }
}
