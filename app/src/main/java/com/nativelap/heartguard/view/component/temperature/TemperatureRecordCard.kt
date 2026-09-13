package com.nativelap.heartguard.view.component.temperature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme

/** 온도계 설치 여부와 수동 입력값을 같은 카드 안에서 관리한다. */
@Composable
fun TemperatureRecordCard(
    temperatureLabel: String,
    temperatureText: String,
    temperatureUnit: String,
    onTemperatureChange: (String) -> Unit,
    installationLabel: String,
    isInstalled: Boolean,
    onInstallationChange: (Boolean) -> Unit,
    checkboxContentDescription: String,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    title: String,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.LargeCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HeartGuardSpacing.Section),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
            )
            TemperatureInputField(
                label = temperatureLabel,
                temperatureText = temperatureText,
                unitLabel = temperatureUnit,
                onTemperatureChange = onTemperatureChange,
                isEnabled = isEnabled,
            )
            TemperatureCheckboxRow(
                label = installationLabel,
                isChecked = isInstalled,
                onCheckedChange = onInstallationChange,
                checkboxContentDescription = checkboxContentDescription,
                isEnabled = isEnabled,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun TemperatureRecordCardPreview() {
    HeartGuardTheme {
        TemperatureRecordCard(
            temperatureLabel = "현재 온도",
            temperatureText = "37",
            temperatureUnit = "℃",
            onTemperatureChange = {},
            installationLabel = "온도계가 설치되어 있습니다",
            isInstalled = true,
            onInstallationChange = {},
            checkboxContentDescription = "온도계 설치 여부",
            title = stringResource(R.string.temperature_manual_input),
        )
    }
}
