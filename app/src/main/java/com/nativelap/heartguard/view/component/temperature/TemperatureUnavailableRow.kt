package com.nativelap.heartguard.view.component.temperature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.extraColors

/** 온도계를 사용할 수 없는 상태를 입력 영역과 구분해 알리는 행이다. */
@Composable
fun TemperatureUnavailableRow(
    title: String,
    description: String,
    warningLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
    ) {
        Text(
            text = warningLabel,
            color = MaterialTheme.extraColors.warning,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
            ),
        )
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TemperatureUnavailableRowPreview() {
    HeartGuardTheme {
        TemperatureUnavailableRow(
            title = "온도계를 사용할 수 없습니다",
            description = "온도계 설치 여부를 확인해 주세요",
            warningLabel = "!",
        )
    }
}
