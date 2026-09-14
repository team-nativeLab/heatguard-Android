package com.nativelap.heartguard.view.route.record

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.view.component.RecordTypeOptionUiModel
import com.nativelap.heartguard.view.component.RecordTypeSelectionSheet
import com.nativelap.heartguard.view.screen.temperature.TemperatureRecordScreen

/** 기록 유형 선택 overlay의 선택 상태를 관리하고 선택 결과를 상위 Navigation에 전달한다. */
@Composable
internal fun HeartGuardRecordTypeSelectionRoute(
    onConfirm: (String) -> Unit,
) {
    var selectedRecordType by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    RecordTypeSelectionSheet(
        title = stringResource(R.string.record_type_selection_title),
        options = listOf(
            RecordTypeOptionUiModel(
                key = "temperature",
                title = stringResource(R.string.record_temperature_title),
                description = stringResource(R.string.record_temperature_description),
                iconPainter = painterResource(R.drawable.record_temperature),
            ),
            RecordTypeOptionUiModel(
                key = "work",
                title = stringResource(R.string.record_work_photo_title),
                description = stringResource(R.string.record_work_photo_description),
                iconPainter = painterResource(R.drawable.record_work_photo),
            ),
            RecordTypeOptionUiModel(
                key = "rest",
                title = stringResource(R.string.record_rest_photo_title),
                description = stringResource(R.string.record_rest_photo_description),
                iconPainter = painterResource(R.drawable.record_rest_photo),
            ),
        ),
        selectedKey = selectedRecordType,
        onOptionSelected = { selectedRecordType = it },
        confirmTitle = stringResource(R.string.common_confirm),
        onConfirm = {
            selectedRecordType?.let(onConfirm)
        },
    )
}

/** 온도·습도 입력 상태를 보유하고 온도 기록 Screen의 로컬 이벤트를 연결한다. */
@Composable
internal fun HeartGuardTemperatureRecordRoute(
    onFieldPhotoClick: () -> Unit,
    onSaveClick: () -> Unit,
) {
    var temperatureText by rememberSaveable {
        mutableStateOf("47.5")
    }
    var humidityText by rememberSaveable {
        mutableStateOf("55")
    }
    var isManualInputEnabled by rememberSaveable {
        mutableStateOf(false)
    }

    TemperatureRecordScreen(
        currentTemperature = "47.5°C",
        humidity = "55%",
        feelsLikeTemperature = "40.5°C",
        temperatureText = temperatureText,
        humidityText = humidityText,
        isManualInputEnabled = isManualInputEnabled,
        onTemperatureChange = { temperatureText = it },
        onHumidityChange = { humidityText = it },
        onManualInputChange = { isManualInputEnabled = it },
        onFieldPhotoClick = onFieldPhotoClick,
        onSaveClick = onSaveClick,
    )
}
