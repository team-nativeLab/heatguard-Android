package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.ui.theme.HeartGuardBorderWidth
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.heatLevelLabelText
import com.nativelap.heartguard.view.component.home.HeatRiskBadge
import com.nativelap.heartguard.view.component.humidityValueText
import com.nativelap.heartguard.view.component.temperatureValueText
import com.nativelap.heartguard.viewmodel.toDisplayNumber

/** 온도계 기록 측정값 카드다(Figma 21). 값은 모두 서버 기록 값이며 없으면 "--"다.
 * 온도계 설치 여부는 서버 필드가 없어, 측정 온도가 기록돼 있으면 설치됨·없으면 미설치로 표시한다. */
@Composable
fun RecordDetailMeasurementCard(
    temperature: Double?,
    humidity: Double?,
    apparentTemperature: Double?,
    heatLevel: Int?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.Card),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(HeartGuardSpacing.Section),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Card),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
            ) {
                RecordTypeIconBox(recordType = FieldRecordType.THERMOMETER)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.history_detail_measured_temperature),
                        color = MaterialTheme.extraColors.secondaryText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
                    ) {
                        Text(
                            text = temperatureValueText(temperature.toDisplayNumber()),
                            color = MaterialTheme.extraColors.strongText,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        if (heatLevel != null) {
                            HeatRiskBadge(riskLabel = heatLevelLabelText(heatLevel))
                        }
                    }
                }
            }

            HorizontalDivider(
                thickness = HeartGuardBorderWidth.Divider,
                color = MaterialTheme.extraColors.cardBorder,
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                MeasurementValue(
                    label = stringResource(R.string.history_detail_humidity),
                    valueText = humidityValueText(humidity.toDisplayNumber()),
                    modifier = Modifier.weight(1f),
                )
                MeasurementValue(
                    label = stringResource(R.string.history_detail_apparent_temperature),
                    valueText = temperatureValueText(apparentTemperature.toDisplayNumber()),
                    modifier = Modifier.weight(1f),
                )
                MeasurementValue(
                    label = stringResource(R.string.history_detail_thermometer),
                    valueText = if (temperature != null) {
                        stringResource(R.string.history_detail_thermometer_installed)
                    } else {
                        stringResource(R.string.history_detail_thermometer_not_installed)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MeasurementValue(
    label: String,
    valueText: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = MaterialTheme.extraColors.secondaryText,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = valueText,
            color = MaterialTheme.extraColors.strongText,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RecordDetailMeasurementCardPreview() {
    HeartGuardTheme {
        RecordDetailMeasurementCard(
            temperature = 36.2,
            humidity = 65.0,
            apparentTemperature = 38.7,
            heatLevel = 1,
        )
    }
}
