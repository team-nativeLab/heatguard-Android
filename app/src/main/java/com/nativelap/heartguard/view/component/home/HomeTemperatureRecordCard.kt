package com.nativelap.heartguard.view.component.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme

/** 홈에서 온도계 직접 입력 상태와 세부 기상 지표를 Figma 카드 구조로 보여준다. */
@Composable
fun HomeTemperatureRecordCard(
    title: String,
    recordHint: String,
    manualInputTitle: String,
    isManualInputEnabled: Boolean,
    onManualInputChange: (Boolean) -> Unit,
    metrics: List<WeatherMetricValue>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(HeartGuardRadius.LargeCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(HeartGuardSpacing.Section),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
        ) {
            // Figma 02_홈_리디자인: 1행은 "데이터 기록" 제목과 안내 문구를 양끝에 배치한다.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(HeartGuardIconSize.Small),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = recordHint,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            // 2행은 "온도계 데이터 직접 입력" 라벨과 Switch를 양끝에 배치한다.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = manualInputTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Switch(
                    checked = isManualInputEnabled,
                    onCheckedChange = onManualInputChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                        uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                )
            }

            // Figma 02_홈_리디자인: 온도/습도/체감온도 값은 연한 배경(surfaceVariant)의 카드로 구분된다.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
            ) {
                metrics.take(3).forEach { metric ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(HeartGuardRadius.PrimaryAction),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        WeatherMetric(
                            metricLabel = metric.label,
                            metricValue = metric.value,
                            modifier = Modifier.padding(HeartGuardSpacing.Item),
                        )
                    }
                }
            }
        }
    }
}

data class WeatherMetricValue(
    val label: String,
    val value: String,
)

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun HomeTemperatureRecordCardPreview() {
    HeartGuardTheme {
        HomeTemperatureRecordCard(
            title = "데이터 기록",
            recordHint = "미설치 시 자동으로 기록됩니다",
            manualInputTitle = "온도계 데이터 직접 입력",
            isManualInputEnabled = false,
            onManualInputChange = {},
            metrics = listOf(
                WeatherMetricValue("온도(°C)", "47.5"),
                WeatherMetricValue("습도(%)", "55"),
                WeatherMetricValue("체감온도(°C)", "자동계산"),
            ),
        )
    }
}
