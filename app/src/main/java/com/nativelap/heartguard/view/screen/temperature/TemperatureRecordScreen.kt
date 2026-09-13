package com.nativelap.heartguard.view.screen.temperature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.temperature.RecordSaveButton
import com.nativelap.heartguard.view.component.temperature.TemperatureRecordCard
import com.nativelap.heartguard.view.component.temperature.TemperatureSummaryCard
import com.nativelap.heartguard.view.component.temperature.TemperatureUnavailableRow

/** 온도 측정값과 온도계 설치 상태를 기록 화면의 세로 흐름으로 조합한다. */
@Composable
fun TemperatureRecordScreen(
    currentTemperature: String,
    humidity: String,
    feelsLikeTemperature: String,
    temperatureText: String,
    isThermometerInstalled: Boolean,
    onTemperatureChange: (String) -> Unit,
    onThermometerInstalledChange: (Boolean) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    isTemperatureInputEnabled: Boolean = true,
    showUnavailableMessage: Boolean = false,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(HeartGuardSpacing.RecordContentHorizontal),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Section),
        ) {
            TemperatureSummaryCard(
                currentTemperature = currentTemperature,
                humidity = humidity,
                feelsLikeTemperature = feelsLikeTemperature,
                currentTemperatureLabel = stringResource(R.string.home_current_temperature),
                humidityLabel = stringResource(R.string.home_humidity),
                feelsLikeLabel = stringResource(R.string.home_feels_like),
                title = stringResource(R.string.temperature_current_measurement),
            )

            TemperatureRecordCard(
                temperatureLabel = stringResource(R.string.temperature_input_label),
                temperatureText = temperatureText,
                temperatureUnit = stringResource(R.string.temperature_input_unit),
                onTemperatureChange = onTemperatureChange,
                installationLabel = if (isThermometerInstalled) {
                    stringResource(R.string.temperature_installed)
                } else {
                    stringResource(R.string.temperature_not_installed)
                },
                isInstalled = isThermometerInstalled,
                onInstallationChange = onThermometerInstalledChange,
                checkboxContentDescription = stringResource(R.string.temperature_checkbox_description),
                isEnabled = isTemperatureInputEnabled,
                title = stringResource(R.string.temperature_manual_input),
            )

            if (showUnavailableMessage) {
                TemperatureUnavailableRow(
                    title = stringResource(R.string.temperature_unavailable),
                    description = stringResource(R.string.temperature_not_installed_description),
                    warningLabel = stringResource(R.string.temperature_warning_label),
                )
            }

            RecordSaveButton(
                title = stringResource(R.string.temperature_save),
                onClick = onSaveClick,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun TemperatureRecordScreenPreview() {
    HeartGuardTheme {
        TemperatureRecordScreen(
            currentTemperature = "37℃",
            humidity = "65%",
            feelsLikeTemperature = "40℃",
            temperatureText = "37",
            isThermometerInstalled = true,
            onTemperatureChange = {},
            onThermometerInstalledChange = {},
            onSaveClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun TemperatureRecordScreenUnavailablePreview() {
    HeartGuardTheme {
        TemperatureRecordScreen(
            currentTemperature = "--",
            humidity = "--",
            feelsLikeTemperature = "--",
            temperatureText = "",
            isThermometerInstalled = true,
            onTemperatureChange = {},
            onThermometerInstalledChange = {},
            onSaveClick = {},
            isTemperatureInputEnabled = false,
            showUnavailableMessage = true,
        )
    }
}
