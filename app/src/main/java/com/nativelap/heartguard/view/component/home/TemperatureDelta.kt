package com.nativelap.heartguard.view.component.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.extraColors

/** 현재 온도와 비교한 변화량을 상승·하락 의미와 함께 표현한다. */
@Composable
fun TemperatureDelta(
    deltaLabel: String,
    deltaValue: String,
    isIncreasing: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        Text(
            text = deltaLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = deltaValue,
            color = if (isIncreasing) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.extraColors.success
            },
            modifier = Modifier.padding(start = HeartGuardSpacing.Tight),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}
