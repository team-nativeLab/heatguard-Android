package com.nativelap.heartguard.view.component.temperature

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 현장 사진 화면에서 저장된 온도계 값을 Figma의 세로 요약 행으로 보여준다. */
@Composable
fun TemperatureInputSummaryCard(
    title: String,
    currentTemperatureLabel: String,
    currentTemperature: String,
    humidityLabel: String,
    humidity: String,
    feelsLikeLabel: String,
    feelsLikeTemperature: String,
    modifier: Modifier = Modifier,
) {
    val summaryRows = listOf(
        TemperatureInputSummaryRow(currentTemperatureLabel, currentTemperature),
        TemperatureInputSummaryRow(humidityLabel, humidity),
        TemperatureInputSummaryRow(feelsLikeLabel, feelsLikeTemperature),
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.Card),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(
            width = HeartGuardSpacing.Hairline,
            color = MaterialTheme.extraColors.cardBorder,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HeartGuardSpacing.Item),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                ),
            )
            summaryRows.forEachIndexed { index, summaryRow ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = HeartGuardSpacing.Tight),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = summaryRow.label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = summaryRow.value,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private data class TemperatureInputSummaryRow(
    val label: String,
    val value: String,
)

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun TemperatureInputSummaryCardPreview() {
    HeartGuardTheme {
        TemperatureInputSummaryCard(
            title = "온도계 데이터 입력",
            currentTemperatureLabel = "온도(°C)",
            currentTemperature = "47.5",
            humidityLabel = "습도(%)",
            humidity = "55",
            feelsLikeLabel = "체감온도(°C)",
            feelsLikeTemperature = "자동계산",
        )
    }
}
