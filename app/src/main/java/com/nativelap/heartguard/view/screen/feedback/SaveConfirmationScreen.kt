package com.nativelap.heartguard.view.screen.feedback

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
import com.nativelap.heartguard.view.component.temperature.RecordSaveButton
import com.nativelap.heartguard.view.component.temperature.TemperatureRecordCard

/** Figma 15_저장전_확인알림에 해당하는 저장 전 확인 화면이다. */
@Composable
fun SaveConfirmationScreen(
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

            PhotoPreviewPlaceholder(
                message = stringResource(R.string.photo_temperature_not_saved),
                onClick = onCaptureClick,
                modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordCardHorizontal),
            )

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
                installationLabel = stringResource(R.string.temperature_not_installed_note),
                isManualInputEnabled = false,
                onManualInputChange = {},
                checkboxContentDescription = stringResource(R.string.temperature_checkbox_description),
                title = stringResource(R.string.temperature_installation_status),
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
            onCaptureClick = {},
            onSaveClick = {},
        )
    }
}
