package com.nativelap.heartguard.view.screen.feedback

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
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.HeartGuardHeader
import com.nativelap.heartguard.view.component.photo.PhotoCaptureRow
import com.nativelap.heartguard.view.component.photo.PhotoPreviewPlaceholder
import com.nativelap.heartguard.view.component.photo.PhotoScreenIntro
import com.nativelap.heartguard.view.component.photo.SelectedPhotoGrid
import com.nativelap.heartguard.view.component.temperature.RecordSaveButton
import com.nativelap.heartguard.view.component.temperature.TemperatureRecordCard

/** Figma 15_저장전_확인알림에 해당하는 저장 전 확인 화면이다.
 * 온도계 기록이 아직 저장되지 않았으면([isTemperatureSaved] false) 경고 상태를, 저장되었으면
 * 실제 입력된 온도·습도 값과 촬영된 현장 사진을 보여준다 — RecordDraftViewModel이 화면 전환 중에도
 * 이 값을 들고 있게 하기 전에는 항상 빈 값만 표시되던 화면이다. */
@Composable
fun SaveConfirmationScreen(
    isTemperatureSaved: Boolean,
    temperatureText: String,
    humidityText: String,
    feelsLikeText: String,
    fieldPhotoUris: List<Uri>,
    onRemoveFieldPhoto: (Uri) -> Unit,
    onCaptureClick: () -> Unit,
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

            PhotoScreenIntro(
                title = stringResource(R.string.photo_field_screen_title),
                description = stringResource(R.string.photo_field_screen_description),
                modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordTitleHorizontal),
            )

            // FieldPhotoScreen과 같은 3단 분기: 촬영된 사진이 있으면 그리드로, 없고 온도계가
            // 아직 저장되지 않았으면 경고 placeholder를, 그 외에는 빈 촬영 유도 placeholder를 보여준다.
            if (fieldPhotoUris.isNotEmpty()) {
                SelectedPhotoGrid(
                    photoUris = fieldPhotoUris,
                    onRemovePhoto = onRemoveFieldPhoto,
                    modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordCardHorizontal),
                )
            } else if (!isTemperatureSaved) {
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

            TemperatureRecordCard(
                temperatureLabel = stringResource(R.string.home_temperature_field),
                temperatureText = if (isTemperatureSaved) temperatureText else "",
                temperatureUnit = "",
                onTemperatureChange = {},
                humidityLabel = stringResource(R.string.home_humidity_field),
                humidityText = if (isTemperatureSaved) humidityText else "",
                humidityUnit = "",
                onHumidityChange = {},
                feelsLikeLabel = stringResource(R.string.home_feels_like_field),
                feelsLikeText = if (isTemperatureSaved) feelsLikeText else "",
                installationLabel = stringResource(R.string.temperature_not_installed_note),
                isManualInputEnabled = false,
                onManualInputChange = {},
                checkboxContentDescription = stringResource(R.string.temperature_checkbox_description),
                title = stringResource(R.string.temperature_installation_status),
                // Figma 15는 이 카드를 항상 읽기 전용 요약으로 보여준다(저장이 확정되면 수정할 수 없다는
                // 안내와 함께) — 값이 채워졌는지와 무관하게 편집은 계속 막아 둔다.
                isCardEnabled = false,
                modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordCardHorizontal),
            )

            PhotoCaptureRow(
                label = stringResource(R.string.photo_field_retry),
                instructionText = stringResource(R.string.photo_field_instruction),
                cameraPainter = painterResource(R.drawable.record_camera),
                cameraContentDescription = stringResource(R.string.photo_capture),
                onClick = onCaptureClick,
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

@Preview(showBackground = true, widthDp = 402, heightDp = 996)
@Composable
private fun SaveConfirmationScreenPreview() {
    HeartGuardTheme {
        SaveConfirmationScreen(
            isTemperatureSaved = false,
            temperatureText = "",
            humidityText = "",
            feelsLikeText = "",
            fieldPhotoUris = emptyList(),
            onRemoveFieldPhoto = {},
            onCaptureClick = {},
            onSaveClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 996)
@Composable
private fun SaveConfirmationScreenSavedPreview() {
    HeartGuardTheme {
        SaveConfirmationScreen(
            isTemperatureSaved = true,
            temperatureText = "47.5",
            humidityText = "55",
            feelsLikeText = "40.5",
            fieldPhotoUris = emptyList(),
            onRemoveFieldPhoto = {},
            onCaptureClick = {},
            onSaveClick = {},
        )
    }
}
