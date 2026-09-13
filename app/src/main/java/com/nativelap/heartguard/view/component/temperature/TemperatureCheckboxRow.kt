package com.nativelap.heartguard.view.component.temperature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.control.HeartGuardCheckbox

/** 온도계 설치 여부처럼 온도 기록과 직접 연결된 선택 행이다. */
@Composable
fun TemperatureCheckboxRow(
    label: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    checkboxContentDescription: String,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HeartGuardComponentSize.TouchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
            ),
        )
        HeartGuardCheckbox(
            isChecked = isChecked,
            onCheckedChange = onCheckedChange,
            contentDescription = checkboxContentDescription,
            isEnabled = isEnabled,
        )
    }
}
