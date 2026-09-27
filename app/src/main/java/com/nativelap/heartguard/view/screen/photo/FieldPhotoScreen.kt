package com.nativelap.heartguard.view.screen.photo

import android.net.Uri
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
import com.nativelap.heartguard.view.component.HeartGuardHeader
import com.nativelap.heartguard.view.component.photo.PhotoCaptureRow
import com.nativelap.heartguard.view.component.photo.PhotoPreviewPlaceholder
import com.nativelap.heartguard.view.component.photo.PhotoScreenIntro
import com.nativelap.heartguard.view.component.photo.SelectedPhotoGrid
import com.nativelap.heartguard.view.component.temperature.RecordSaveButton
import com.nativelap.heartguard.view.component.temperature.TemperatureInputSummaryCard

/** 현장 사진 촬영 전·저장 전 오류 상태를 Figma 화면 구조로 조합한다. */
@Composable
fun FieldPhotoScreen(
    currentTemperature: String,
    humidity: String,
    feelsLikeTemperature: String,
    selectedPhotoCount: Int,
    selectedPhotoUris: List<Uri> = emptyList(),
    onRemovePhoto: (Uri) -> Unit = {},
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
                // 메뉴·알림 기능은 Figma/API 명세서 어디에도 정의되어 있지 않아 의도적으로 비워둔다.
                onMenuClick = {},
                onNotificationClick = {},
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Section),
            ) {
                PhotoScreenIntro(
                    title = stringResource(R.string.photo_field_screen_title),
                    description = stringResource(R.string.photo_field_screen_description),
                    modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordTitleHorizontal),
                )

                // Figma "14_현장사진_촬영전"은 온도계 데이터 요약보다 먼저 오는 큰 사진 선택
                // 영역을 별도로 갖고 있다. 기존 구현에는 이 영역이 빠져 있어 추가했다.
                // "15_저장전_확인알림"은 별도 팝업이 아니라 이 화면의 상태 변형으로, 같은 박스
                // 안에서 카메라 아이콘 대신 X 아이콘 + "온도계가 아직 저장이 안되었어요" 문구가
                // 표시된다. 두 카드를 함께 보여주지 않고 상태에 따라 하나만 표시한다.
                if (selectedPhotoUris.isNotEmpty()) {
                    SelectedPhotoGrid(
                        photoUris = selectedPhotoUris,
                        onRemovePhoto = onRemovePhoto,
                        modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordCardHorizontal),
                    )
                } else if (showTemperatureSaveError) {
                    PhotoPreviewPlaceholder(
                        message = stringResource(R.string.photo_temperature_not_saved),
                        onClick = onCaptureClick,
                        modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordCardHorizontal),
                    )
                } else {
                    PhotoPreviewPlaceholder(
                        onClick = onCaptureClick,
                        modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordCardHorizontal),
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
                    modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordCardHorizontal),
                )

                PhotoCaptureRow(
                    label = stringResource(R.string.photo_field_retry),
                    instructionText = stringResource(R.string.photo_field_instruction),
                    cameraPainter = painterResource(R.drawable.record_camera),
                    cameraContentDescription = stringResource(R.string.photo_capture),
                    onClick = onCaptureClick,
                    isEnabled = selectedPhotoCount < MAX_RECORD_PHOTO_COUNT,
                    modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordCardHorizontal),
                )

                RecordSaveButton(
                    title = stringResource(R.string.temperature_save),
                    onClick = onSaveClick,
                    modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordContentHorizontal),
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
