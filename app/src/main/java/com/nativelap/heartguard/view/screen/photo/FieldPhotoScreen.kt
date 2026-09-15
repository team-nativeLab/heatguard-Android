package com.nativelap.heartguard.view.screen.photo

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
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.HeartGuardHeader
import com.nativelap.heartguard.view.component.photo.PhotoCaptureRow
import com.nativelap.heartguard.view.component.photo.PhotoScreenIntro
import com.nativelap.heartguard.view.component.photo.TemperatureDataErrorCard
import com.nativelap.heartguard.view.component.temperature.RecordSaveButton
import com.nativelap.heartguard.view.component.temperature.TemperatureInputSummaryCard

/** 현장 사진 촬영 전·저장 전 오류 상태를 Figma 화면 구조로 조합한다. */
@Composable
fun FieldPhotoScreen(
    currentTemperature: String,
    humidity: String,
    feelsLikeTemperature: String,
    // TODO: Figma "14_현장사진_촬영전" 프레임의 재촬영 박스는 선택 개수 라벨을 표시하지 않는다.
    // PhotoRoutes.kt 연결은 손대지 않는 범위라 매개변수는 유지하되 이 화면에서는 사용하지 않는다.
    @Suppress("UNUSED_PARAMETER") selectedPhotoCount: Int,
    showTemperatureSaveError: Boolean,
    onCaptureClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        // 헤더는 자체 여백(HeartGuardHeader의 HeaderHorizontal)으로 좌우 아이콘 위치를 관리하므로,
        // 화면 전체에 가로 패딩을 주지 않고 헤더 아래 콘텐츠에만 별도로 적용한다.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Section),
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
                verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Section),
            ) {
                PhotoScreenIntro(
                    title = stringResource(R.string.photo_field_screen_title),
                    description = stringResource(R.string.photo_field_screen_description),
                )

                if (showTemperatureSaveError) {
                    TemperatureDataErrorCard(
                        message = stringResource(R.string.photo_temperature_not_saved),
                    )
                }

                TemperatureInputSummaryCard(
                    currentTemperature = currentTemperature,
                    humidity = humidity,
                    feelsLikeTemperature = feelsLikeTemperature,
                    currentTemperatureLabel = stringResource(R.string.home_current_temperature),
                    humidityLabel = stringResource(R.string.home_humidity),
                    feelsLikeLabel = stringResource(R.string.home_feels_like),
                    title = stringResource(R.string.temperature_installation_status),
                )

                PhotoCaptureRow(
                    label = stringResource(R.string.photo_field_retry),
                    instructionText = stringResource(R.string.photo_field_instruction),
                    cameraPainter = painterResource(R.drawable.record_camera),
                    cameraContentDescription = stringResource(R.string.photo_capture),
                    onClick = onCaptureClick,
                )

                RecordSaveButton(
                    title = stringResource(R.string.temperature_save),
                    onClick = onSaveClick,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 996)
@Composable
private fun FieldPhotoScreenPreview() {
    HeartGuardTheme {
        FieldPhotoScreen(
            currentTemperature = "47.5°C",
            humidity = "55%",
            feelsLikeTemperature = "40.5°C",
            selectedPhotoCount = 0,
            showTemperatureSaveError = false,
            onCaptureClick = {},
            onSaveClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 996)
@Composable
private fun FieldPhotoScreenErrorPreview() {
    HeartGuardTheme {
        FieldPhotoScreen(
            currentTemperature = "47.5°C",
            humidity = "55%",
            feelsLikeTemperature = "40.5°C",
            selectedPhotoCount = 1,
            showTemperatureSaveError = true,
            onCaptureClick = {},
            onSaveClick = {},
        )
    }
}
