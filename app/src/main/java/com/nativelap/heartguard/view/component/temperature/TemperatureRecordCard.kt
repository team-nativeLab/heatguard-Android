package com.nativelap.heartguard.view.component.temperature

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardBorderWidth
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 온도계 데이터 직접 입력 Switch와 세 개의 입력 박스를 Figma 카드 구조로 제공한다. */
@Composable
fun TemperatureRecordCard(
    temperatureLabel: String,
    temperatureText: String,
    temperatureUnit: String,
    onTemperatureChange: (String) -> Unit,
    humidityLabel: String,
    humidityText: String,
    humidityUnit: String,
    onHumidityChange: (String) -> Unit,
    feelsLikeLabel: String,
    feelsLikeText: String,
    installationLabel: String,
    isManualInputEnabled: Boolean,
    onManualInputChange: (Boolean) -> Unit,
    checkboxContentDescription: String,
    modifier: Modifier = Modifier,
    title: String,
    isCardEnabled: Boolean = true,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .alpha(
                    if (isCardEnabled) {
                        ENABLED_ALPHA
                    } else {
                        DISABLED_ALPHA
                    },
                ),
        shape = RoundedCornerShape(HeartGuardRadius.Card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border =
            BorderStroke(
                width = HeartGuardBorderWidth.Divider,
                color = MaterialTheme.extraColors.cardBorder,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(HeartGuardSpacing.Section),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    style =
                        MaterialTheme.typography.titleSmall.copy(
                            fontSize = HeartGuardFontSize.CardTitle,
                            fontWeight = FontWeight.Bold,
                        ),
                )
                Switch(
                    modifier =
                        Modifier.semantics {
                            contentDescription = checkboxContentDescription
                        },
                    checked = isManualInputEnabled,
                    onCheckedChange = onManualInputChange,
                    enabled = isCardEnabled,
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                            uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
            ) {
                TemperatureInputField(
                    label = temperatureLabel,
                    temperatureText = temperatureText,
                    unitLabel = temperatureUnit,
                    onTemperatureChange = onTemperatureChange,
                    isEnabled = isCardEnabled && isManualInputEnabled,
                    placeholderText = stringResource(R.string.temperature_input_placeholder),
                    modifier = Modifier.weight(1f),
                )
                TemperatureInputField(
                    label = humidityLabel,
                    temperatureText = humidityText,
                    unitLabel = humidityUnit,
                    onTemperatureChange = onHumidityChange,
                    isEnabled = isCardEnabled && isManualInputEnabled,
                    placeholderText = stringResource(R.string.humidity_input_placeholder),
                    modifier = Modifier.weight(1f),
                )
                TemperatureInputField(
                    label = feelsLikeLabel,
                    temperatureText = feelsLikeText,
                    unitLabel = "",
                    onTemperatureChange = {},
                    isEnabled = false,
                    placeholderText = stringResource(R.string.temperature_auto_calculated),
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = installationLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun TemperatureRecordCardPreview() {
    HeartGuardTheme {
        TemperatureRecordCard(
            temperatureLabel = "온도 (°C)",
            temperatureText = "",
            temperatureUnit = "",
            onTemperatureChange = {},
            humidityLabel = "습도 (%)",
            humidityText = "",
            humidityUnit = "",
            onHumidityChange = {},
            feelsLikeLabel = "체감온도 (°C)",
            feelsLikeText = "",
            installationLabel = "미설치 시 자동으로 기록됩니다.",
            isManualInputEnabled = false,
            onManualInputChange = {},
            checkboxContentDescription = "온도계 데이터 직접 입력",
            title = "온도계 데이터 직접 입력",
        )
    }
}

private const val ENABLED_ALPHA = 1f

// Figma 15_저장전_확인알림의 비활성 카드(opacity 50%)와 같은 투명도다.
private const val DISABLED_ALPHA = 0.5f
