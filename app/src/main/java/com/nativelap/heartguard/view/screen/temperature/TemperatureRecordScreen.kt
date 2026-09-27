package com.nativelap.heartguard.view.screen.temperature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.nativelap.heartguard.domain.record.model.MAX_RECORD_PHOTO_COUNT
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.BottomActionBar
import com.nativelap.heartguard.view.component.HeartGuardHeader
import com.nativelap.heartguard.view.component.photo.PhotoCaptureRow
import com.nativelap.heartguard.view.component.temperature.RecordSaveButton
import com.nativelap.heartguard.view.component.temperature.TemperatureRecordCard
import com.nativelap.heartguard.view.component.temperature.TemperatureScreenIntro
import com.nativelap.heartguard.view.component.temperature.TemperatureSummaryCard

/** Figma 06_온도계 기록 입력 / 13_온도계미설치_체크됨 화면이다.
 * 상단에 현장 현재 온도를, 아래에 "온도계 데이터 직접 입력" 카드와 현장 사진 진입 행을 두고, 저장 버튼은 하단에 고정한다.
 * 직접 입력을 켜면(온도계 미설치) 현장 사진 행을 비활성화한다. */
@Composable
fun TemperatureRecordScreen(
    currentTemperature: String,
    humidity: String,
    feelsLikeTemperature: String,
    temperatureText: String,
    humidityText: String,
    isManualInputEnabled: Boolean,
    selectedFieldPhotoCount: Int,
    isSaveEnabled: Boolean,
    onTemperatureChange: (String) -> Unit,
    onHumidityChange: (String) -> Unit,
    onManualInputChange: (Boolean) -> Unit,
    onFieldPhotoClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.extraColors.pageBackground,
        bottomBar = {
            BottomActionBar {
                RecordSaveButton(
                    title = stringResource(R.string.temperature_save),
                    onClick = onSaveClick,
                    enabled = isSaveEnabled,
                )
            }
        },
    ) { innerPadding ->
        // 헤더는 자체 여백(HeartGuardHeader의 HeaderHorizontal)으로 좌우 아이콘 위치를 관리하므로,
        // 화면 전체에 가로 패딩을 주지 않고 헤더 아래 콘텐츠에만 별도로 적용한다.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Section),
        ) {
            HeartGuardHeader(
                title = stringResource(R.string.brand_name),
                menuPainter = painterResource(R.drawable.menu_hamburger),
                notificationPainter = painterResource(R.drawable.notification_bell),
                // 메뉴·알림 기능은 홈 화면에서만 제공한다.
                onMenuClick = {},
                onNotificationClick = {},
            )

            TemperatureScreenIntro(
                title = stringResource(R.string.temperature_screen_title),
                description = stringResource(R.string.temperature_screen_description),
                modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordTitleHorizontal),
            )

            TemperatureSummaryCard(
                currentTemperature = currentTemperature,
                humidity = humidity,
                feelsLikeTemperature = feelsLikeTemperature,
                currentTemperatureLabel = stringResource(R.string.home_current_temperature),
                humidityLabel = stringResource(R.string.home_humidity),
                feelsLikeLabel = stringResource(R.string.temperature_feels_like_short),
                title = stringResource(R.string.temperature_current_measurement),
                thermometerPainter = painterResource(R.drawable.record_thermometer_illustration),
            )

            TemperatureRecordCard(
                temperatureLabel = stringResource(R.string.home_temperature_field),
                temperatureText = if (isManualInputEnabled) {
                    temperatureText
                } else {
                    ""
                },
                temperatureUnit = "",
                onTemperatureChange = onTemperatureChange,
                humidityLabel = stringResource(R.string.home_humidity_field),
                humidityText = if (isManualInputEnabled) {
                    humidityText
                } else {
                    ""
                },
                humidityUnit = "",
                onHumidityChange = onHumidityChange,
                feelsLikeLabel = stringResource(R.string.home_feels_like_field),
                // 체감온도는 저장할 때 서버가 계산하므로 입력 칸에는 값 대신 "자동 계산" 안내만 보여준다.
                feelsLikeText = "",
                installationLabel = stringResource(R.string.temperature_not_installed_note),
                isManualInputEnabled = isManualInputEnabled,
                onManualInputChange = onManualInputChange,
                checkboxContentDescription = stringResource(R.string.temperature_checkbox_description),
                title = stringResource(R.string.temperature_manual_input_title),
                modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordCardHorizontal),
            )

            PhotoCaptureRow(
                label = stringResource(R.string.home_field_photo),
                instructionText = stringResource(R.string.photo_field_instruction_two_line),
                cameraPainter = painterResource(R.drawable.record_camera),
                cameraContentDescription = stringResource(R.string.photo_capture),
                onClick = onFieldPhotoClick,
                isEnabled = !isManualInputEnabled && selectedFieldPhotoCount < MAX_RECORD_PHOTO_COUNT,
                modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordCardHorizontal),
                labelHorizontalOffset = HeartGuardSpacing.RecordTitleHorizontal - HeartGuardSpacing.RecordCardHorizontal,
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
            temperatureText = "",
            humidityText = "",
            isManualInputEnabled = false,
            selectedFieldPhotoCount = 0,
            isSaveEnabled = false,
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
            currentTemperature = "--",
            humidity = "--",
            feelsLikeTemperature = "--",
            temperatureText = "47.5",
            humidityText = "55",
            isManualInputEnabled = true,
            selectedFieldPhotoCount = 0,
            isSaveEnabled = true,
            onTemperatureChange = {},
            onHumidityChange = {},
            onManualInputChange = {},
            onFieldPhotoClick = {},
            onSaveClick = {},
        )
    }
}
