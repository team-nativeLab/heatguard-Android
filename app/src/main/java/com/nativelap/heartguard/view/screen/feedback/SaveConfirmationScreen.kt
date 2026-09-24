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

            // save_confirmation_*는 이 화면 전용으로 정의돼 있었지만 그동안 photo_field_screen_*
            // (현장 사진 화면 문구)를 대신 재사용하고 있었다. 온도계 기록이 이미 저장된 상태(다시
            // 확인만 하면 되는 경우)와 아직 저장되지 않은 상태(먼저 저장부터 해야 하는 경우)의
            // 문구가 서로 다르게 준비되어 있어, 기존 isTemperatureSaved 분기에 맞춰 연결한다.
            PhotoScreenIntro(
                title = if (isTemperatureSaved) {
                    stringResource(R.string.save_confirmation_title)
                } else {
                    stringResource(R.string.save_confirmation_unsaved_title)
                },
                description = if (isTemperatureSaved) {
                    stringResource(R.string.save_confirmation_message)
                } else {
                    stringResource(R.string.save_confirmation_unsaved_description)
                },
                modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordTitleHorizontal),
            )

            // 온도계 저장 여부를 먼저 확인한다 — 이 화면은 "온도계를 먼저 저장하라"는 게이트가
            // 핵심이므로, 사진이 이미 있더라도 온도계가 아직 저장되지 않았다면 경고를 우선 보여준다.
            // (사진만 먼저 찍고 TemperatureRecord의 "저장"은 누르지 않은 채 FieldPhoto에서 바로
            // 저장을 시도하는 경로가 있어, 사진 유무만으로 분기하면 이 경고가 가려질 수 있었다.)
            if (!isTemperatureSaved) {
                PhotoPreviewPlaceholder(
                    message = stringResource(R.string.photo_temperature_not_saved),
                    onClick = onCaptureClick,
                    modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordCardHorizontal),
                )
            } else if (fieldPhotoUris.isNotEmpty()) {
                SelectedPhotoGrid(
                    photoUris = fieldPhotoUris,
                    onRemovePhoto = onRemoveFieldPhoto,
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
