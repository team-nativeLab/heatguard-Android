package com.nativelap.heartguard.view.screen.temperature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // 시스템 바를 제외한 높이가 짧은 기기에서는 섹션 간격만 줄이고, 콘텐츠는 계속 세로 스크롤한다.
            val sectionSpacing = if (
                maxHeight < HeartGuardComponentSize.CompactScreenHeightBreakpoint
            ) {
                HeartGuardSpacing.CompactSection
            } else {
                HeartGuardSpacing.Section
            }

            // 헤더는 자체 여백(HeartGuardHeader의 HeaderHorizontal)으로 좌우 아이콘 위치를 관리하므로,
            // 화면 전체에 가로 패딩을 주지 않고 헤더 아래 콘텐츠에만 별도로 적용한다.
            // (Column 전체에 가로 패딩을 주면 헤더의 자체 여백과 겹쳐 아이콘이 더 안쪽으로 밀린다.)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(sectionSpacing),
            ) {
                HeartGuardHeader(
                    title = stringResource(R.string.brand_name),
                    menuPainter = painterResource(R.drawable.menu_hamburger),
                    notificationPainter = painterResource(R.drawable.notification_bell),
                    onMenuClick = {},
                    onNotificationClick = {},
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HeartGuardSpacing.ScreenHorizontal),
                    verticalArrangement = Arrangement.spacedBy(sectionSpacing),
                ) {
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
                        thermometerPainter = painterResource(R.drawable.record_temperature),
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

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
                    ) {
                        Text(
                            text = stringResource(R.string.home_field_photo),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        PhotoSelectionCard(
                            title = stringResource(R.string.photo_field_instruction),
                            description = null,
                            cameraPainter = painterResource(R.drawable.record_camera),
                            cameraContentDescription = stringResource(R.string.photo_capture),
                            onClick = onFieldPhotoClick,
                            isEnabled = !isManualInputEnabled,
                            minHeight = HeartGuardComponentSize.FieldPhotoSelectionHeight,
                        )
                    }

                    RecordSaveButton(
                        title = stringResource(R.string.temperature_save),
                        onClick = onSaveClick,
                    )
                }
            }
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
