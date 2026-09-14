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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.HeartGuardHeader
import com.nativelap.heartguard.view.component.photo.PhotoSelectionCard
import com.nativelap.heartguard.view.component.temperature.RecordSaveButton
import com.nativelap.heartguard.view.component.temperature.TemperatureRecordCard
import com.nativelap.heartguard.view.component.temperature.TemperatureScreenIntro
import com.nativelap.heartguard.view.component.temperature.TemperatureSummaryCard

/** 온도계 데이터와 현장 사진 기록 진입을 Figma 온도 기록 화면으로 조합한다. */
@Composable
fun TemperatureRecordScreen(
    currentTemperature: String,
    humidity: String,
    feelsLikeTemperature: String,
    temperatureText: String,
    humidityText: String,
    isManualInputEnabled: Boolean,
    onTemperatureChange: (String) -> Unit,
    onHumidityChange: (String) -> Unit,
    onManualInputChange: (Boolean) -> Unit,
    onFieldPhotoClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
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
                .padding(horizontal = HeartGuardSpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Section),
        ) {
            HeartGuardHeader(
                title = stringResource(R.string.brand_name),
                menuPainter = painterResource(R.drawable.menu_hamburger),
                notificationPainter = painterResource(R.drawable.notification_bell),
                onMenuClick = {},
                onNotificationClick = {},
            )

            TemperatureScreenIntro(
                title = stringResource(R.string.temperature_screen_title),
                description = stringResource(R.string.temperature_screen_description),
            )

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
                temperatureLabel = stringResource(R.string.home_temperature_field),
                temperatureText = temperatureText,
                temperatureUnit = stringResource(R.string.home_temperature_unit),
                onTemperatureChange = onTemperatureChange,
                humidityLabel = stringResource(R.string.home_humidity_field),
                humidityText = humidityText,
                humidityUnit = stringResource(R.string.home_percent_unit),
                onHumidityChange = onHumidityChange,
                feelsLikeLabel = stringResource(R.string.home_feels_like_field),
                feelsLikeText = if (isManualInputEnabled) {
                    feelsLikeTemperature
                } else {
                    stringResource(R.string.home_calculated)
                },
                installationLabel = stringResource(R.string.temperature_not_installed),
                isManualInputEnabled = isManualInputEnabled,
                onManualInputChange = onManualInputChange,
                checkboxContentDescription = stringResource(R.string.temperature_checkbox_description),
                title = stringResource(R.string.temperature_installation_status),
            )

            PhotoSelectionCard(
                title = stringResource(R.string.home_field_photo),
                description = stringResource(R.string.photo_field_instruction),
                cameraPainter = painterResource(R.drawable.record_camera),
                cameraContentDescription = stringResource(R.string.photo_capture),
                onClick = onFieldPhotoClick,
                minHeight = HeartGuardComponentSize.FieldPhotoSelectionHeight,
            )

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
            currentTemperature = "47.5°C",
            humidity = "55%",
            feelsLikeTemperature = "40.5°C",
            temperatureText = "47.5",
            humidityText = "55",
            isManualInputEnabled = false,
            onTemperatureChange = {},
            onHumidityChange = {},
            onManualInputChange = {},
            onFieldPhotoClick = {},
            onSaveClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun TemperatureRecordScreenCheckedPreview() {
    HeartGuardTheme {
        TemperatureRecordScreen(
            currentTemperature = "47.5°C",
            humidity = "55%",
            feelsLikeTemperature = "40.5°C",
            temperatureText = "47.5",
            humidityText = "55",
            isManualInputEnabled = true,
            onTemperatureChange = {},
            onHumidityChange = {},
            onManualInputChange = {},
            onFieldPhotoClick = {},
            onSaveClick = {},
        )
    }
}
