package com.nativelap.heartguard.view.route.record

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.view.component.RecordType
import com.nativelap.heartguard.view.component.RecordTypeSelectionSheet
import com.nativelap.heartguard.view.component.recordTypeOptions
import com.nativelap.heartguard.view.screen.temperature.TemperatureRecordScreen
import com.nativelap.heartguard.viewmodel.record.RecordDraftViewModel

/** 기록 유형 선택 overlay의 선택 상태를 관리하고 선택 결과를 상위 Navigation에 전달한다.
 * 새 기록을 시작하는 진입점이므로, 진입 시 [recordDraftViewModel]을 초기화해 이전 시도의
 * 입력값·임시 사진 파일이 남아있지 않게 한다. */
@Composable
internal fun HeartGuardRecordTypeSelectionRoute(
    recordDraftViewModel: RecordDraftViewModel,
    onConfirm: (RecordType) -> Unit,
) {
    var selectedRecordType by rememberSaveable {
        mutableStateOf<RecordType?>(null)
    }

    LaunchedEffect(Unit) {
        recordDraftViewModel.reset()
    }

    RecordTypeSelectionSheet(
        title = stringResource(R.string.record_type_selection_title),
        options = recordTypeOptions(),
        selectedKey = selectedRecordType,
        onOptionSelected = { selectedRecordType = it },
        confirmTitle = stringResource(R.string.common_confirm),
        onConfirm = {
            selectedRecordType?.let(onConfirm)
        },
    )
}

/** 온도·습도 입력 상태를 [recordDraftViewModel]과 공유해 저장 전 확인 화면까지 값이 유지되게 한다. */
@Composable
internal fun HeartGuardTemperatureRecordRoute(
    recordDraftViewModel: RecordDraftViewModel,
    onFieldPhotoClick: () -> Unit,
    onSaveClick: () -> Unit,
) {
    val draftState by recordDraftViewModel.uiState.collectAsState()

    TemperatureRecordScreen(
        currentTemperature = "47.5°C",
        humidity = "55%",
        feelsLikeTemperature = "40.5°C",
        temperatureText = draftState.temperatureText,
        humidityText = draftState.humidityText,
        isManualInputEnabled = draftState.isManualInputEnabled,
        onTemperatureChange = { newValue ->
            recordDraftViewModel.updateTemperatureInput(
                temperatureText = newValue,
                humidityText = draftState.humidityText,
                isManualInputEnabled = draftState.isManualInputEnabled,
            )
        },
        onHumidityChange = { newValue ->
            recordDraftViewModel.updateTemperatureInput(
                temperatureText = draftState.temperatureText,
                humidityText = newValue,
                isManualInputEnabled = draftState.isManualInputEnabled,
            )
        },
        onManualInputChange = { newValue ->
            recordDraftViewModel.updateTemperatureInput(
                temperatureText = draftState.temperatureText,
                humidityText = draftState.humidityText,
                isManualInputEnabled = newValue,
            )
        },
        onFieldPhotoClick = onFieldPhotoClick,
        onSaveClick = {
            recordDraftViewModel.markTemperatureSaved()
            onSaveClick()
        },
    )
}
