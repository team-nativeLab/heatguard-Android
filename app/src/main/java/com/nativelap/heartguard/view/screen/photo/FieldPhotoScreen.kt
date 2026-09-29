package com.nativelap.heartguard.view.screen.photo

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.record.model.MAX_RECORD_PHOTO_COUNT
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.BottomActionBar
import com.nativelap.heartguard.view.component.HeartGuardHeader
import com.nativelap.heartguard.view.component.photo.PhotoCaptureRow
import com.nativelap.heartguard.view.component.photo.PhotoPreviewPlaceholder
import com.nativelap.heartguard.view.component.photo.PhotoScreenIntro
import com.nativelap.heartguard.view.component.photo.SelectedPhotoGrid
import com.nativelap.heartguard.view.component.temperature.RecordSaveButton
import com.nativelap.heartguard.view.component.temperature.TemperatureInputSummaryCard
import com.nativelap.heartguard.view.component.temperature.TemperatureRecordCard

/** Figma 14_현장사진_촬영전 / 15_저장전_확인알림 화면이다.
 * 14: 촬영 전에는 빈 사진 미리보기, 고른 뒤에는 사진 목록과 직접 입력한 온도계 값 요약을 보여준다.
 * 15: 사진 없이 저장을 누르면([showSaveError]) 같은 화면에서 미리보기 자리에 "온도계가 아직 저장이 안되었어요"를 띄우고,
 * 직접 입력 카드를 비활성 상태로 보여준다. 저장 버튼은 하단에 고정한다. */
@Composable
fun FieldPhotoScreen(
    manualTemperature: String,
    manualHumidity: String,
    selectedPhotoUris: List<Uri>,
    showSaveError: Boolean,
    isSaveEnabled: Boolean,
    onCaptureClick: () -> Unit,
    onRemovePhoto: (Uri) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.extraColors.pageBackground,
        bottomBar = {
            BottomActionBar {
                RecordSaveButton(
                    title = stringResource(R.string.record_save_short),
                    onClick = onSaveClick,
                    enabled = isSaveEnabled,
                )
            }
        },
    ) { innerPadding ->
        // 헤더는 자체 여백(HeartGuardHeader의 HeaderHorizontal)으로 좌우 아이콘 위치를 관리하므로,
        // 화면 전체에 가로 패딩을 주지 않고 헤더 아래 콘텐츠에만 별도로 적용한다.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Section),
            ) {
            HeartGuardHeader(
                title = stringResource(R.string.brand_name),
                menuPainter = painterResource(R.drawable.menu_hamburger),
                notificationPainter = painterResource(R.drawable.notification_bell),
                onMenuClick = onMenuClick,
                onNotificationClick = onNotificationClick,
            )

            PhotoScreenIntro(
                title = stringResource(R.string.photo_field_screen_title),
                description = stringResource(R.string.photo_field_screen_description),
                modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordTitleHorizontal),
            )

            if (selectedPhotoUris.isNotEmpty()) {
                SelectedPhotoGrid(
                    photoUris = selectedPhotoUris,
                    onRemovePhoto = onRemovePhoto,
                    modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordCardHorizontal),
                )
            } else {
                PhotoPreviewPlaceholder(
                    message = if (showSaveError) {
                        stringResource(R.string.photo_temperature_not_saved)
                    } else {
                        null
                    },
                    onClick = onCaptureClick,
                    modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordCardHorizontal),
                )
            }

            if (showSaveError) {
                TemperatureRecordCard(
                    temperatureLabel = stringResource(R.string.home_temperature_field),
                    temperatureText = "",
                    temperatureUnit = "",
                    onTemperatureChange = {},
                    humidityLabel = stringResource(R.string.home_humidity_field),
                    humidityText = "",
                    humidityUnit = "",
                    onHumidityChange = {},
                    feelsLikeLabel = stringResource(R.string.home_feels_like_field),
                    feelsLikeText = "",
                    installationLabel = stringResource(R.string.temperature_save_locked_note),
                    isManualInputEnabled = false,
                    onManualInputChange = {},
                    checkboxContentDescription = stringResource(R.string.temperature_checkbox_description),
                    title = stringResource(R.string.temperature_manual_input_title),
                    isCardEnabled = false,
                    modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordCardHorizontal),
                )
            } else {
                TemperatureInputSummaryCard(
                    title = stringResource(R.string.temperature_installation_status),
                    currentTemperatureLabel = stringResource(R.string.home_temperature_field),
                    currentTemperature = manualTemperature,
                    humidityLabel = stringResource(R.string.home_humidity_field),
                    humidity = manualHumidity,
                    feelsLikeLabel = stringResource(R.string.home_feels_like_field),
                    // 체감온도는 저장할 때 서버가 계산한다.
                    feelsLikeTemperature = stringResource(R.string.temperature_auto_calculated),
                    modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordCardHorizontal),
                )
            }

            PhotoCaptureRow(
                label = stringResource(R.string.photo_field_retry),
                instructionText = stringResource(R.string.photo_field_instruction_two_line),
                cameraPainter = painterResource(R.drawable.record_camera),
                cameraContentDescription = stringResource(R.string.photo_capture),
                onClick = onCaptureClick,
                isEnabled = selectedPhotoUris.size < MAX_RECORD_PHOTO_COUNT,
                modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordCardHorizontal),
                labelHorizontalOffset = HeartGuardSpacing.RecordTitleHorizontal - HeartGuardSpacing.RecordCardHorizontal,
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
            manualTemperature = "47.5",
            manualHumidity = "55",
            selectedPhotoUris = emptyList(),
            showSaveError = false,
            isSaveEnabled = true,
            onCaptureClick = {},
            onRemovePhoto = {},
            onSaveClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 996)
@Composable
private fun FieldPhotoScreenSaveErrorPreview() {
    HeartGuardTheme {
        FieldPhotoScreen(
            manualTemperature = "--",
            manualHumidity = "--",
            selectedPhotoUris = emptyList(),
            showSaveError = true,
            isSaveEnabled = true,
            onCaptureClick = {},
            onRemovePhoto = {},
            onSaveClick = {},
        )
    }
}
