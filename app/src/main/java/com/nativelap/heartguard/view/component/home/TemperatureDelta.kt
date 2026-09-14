package com.nativelap.heartguard.view.component.home

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.extraColors

/** 현재 온도와 비교한 변화량을 Figma의 강조 pill과 접근성 설명으로 표현한다. */
@Composable
fun TemperatureDelta(
    deltaLabel: String,
    deltaValue: String,
    isIncreasing: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .widthIn(min = HeartGuardComponentSize.TemperatureDeltaMinWidth)
            .semantics {
                contentDescription = "$deltaLabel $deltaValue"
            },
        shape = RoundedCornerShape(HeartGuardRadius.Pill),
        color = if (isIncreasing) {
            MaterialTheme.extraColors.alertContainer
        } else {
            MaterialTheme.extraColors.successContainer
        },
        contentColor = if (isIncreasing) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.extraColors.success
        },
    ) {
        Text(
            text = deltaValue,
            modifier = Modifier.padding(
                horizontal = HeartGuardSpacing.Compact,
                vertical = HeartGuardSpacing.Tight,
            ),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}
