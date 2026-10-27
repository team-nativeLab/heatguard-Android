package com.nativelap.heartguard.view.route.record

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nativelap.heartguard.R
import com.nativelap.heartguard.view.component.RecordType
import com.nativelap.heartguard.view.component.RecordTypeSelectionSheet
import com.nativelap.heartguard.view.component.humidityValueText
import com.nativelap.heartguard.view.component.recordTypeOptions
import com.nativelap.heartguard.view.component.temperatureValueText
import com.nativelap.heartguard.view.screen.temperature.TemperatureRecordScreen
import com.nativelap.heartguard.viewmodel.home.HomeViewModel
import com.nativelap.heartguard.viewmodel.record.RecordDraftViewModel
import com.nativelap.heartguard.viewmodel.record.RecordSubmissionState
import com.nativelap.heartguard.viewmodel.record.canAttemptSubmission

/** 기록 유형 선택 overlay의 선택 상태를 관리하고 선택 결과를 상위 Navigation에 전달한다.
 * 임시 저장 초안이 있으면 같은 유형을 선택했을 때 이어가고, 다른 유형을 선택하면 정리한다. */
@Composable
internal fun HeartGuardRecordTypeSelectionRoute(
    recordDraftViewModel: RecordDraftViewModel,
    onConfirm: (RecordType) -> Unit,
) {
    var selectedRecordType by rememberSaveable {
        mutableStateOf<RecordType?>(null)
    }

    LaunchedEffect(Unit) {
        if (!recordDraftViewModel.uiState.value.isTemporarilySaved) {
            recordDraftViewModel.reset()
        }
    }

    RecordTypeSelectionSheet(
        title = stringResource(R.string.record_type_selection_title),
        options = recordTypeOptions(),
        selectedKey = selectedRecordType,
        onOptionSelected = { selectedRecordType = it },
        confirmTitle = stringResource(R.string.common_confirm),
        onConfirm = {
            selectedRecordType?.let { recordType ->
                recordDraftViewModel.startRecord(recordType)
                onConfirm(recordType)
            }
        },
    )
}

/** 온도계 기록 화면이다. 상단 현재 온도는 작업자 홈 응답 값이며(없으면 "--"), 직접 입력 값은 [recordDraftViewModel]과 공유한다.
 * "기록 저장"은 확인 화면 없이 바로 기록을 등록하고 결과에 따라 [onSaveSuccess]/[onSaveFailure]로 이동한다.
 * 직접 입력 온도·습도와 함께 현장 사진을 첨부해 저장한다. */
@Composable
internal fun HeartGuardTemperatureRecordRoute(
    recordDraftViewModel: RecordDraftViewModel,
    homeViewModel: HomeViewModel,
    onFieldPhotoClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    onSaveFailure: () -> Unit,
    onMenuClick: () -> Unit,
    onNotificationClick: () -> Unit,
) {
    val draftState by recordDraftViewModel.uiState.collectAsStateWithLifecycle()
    val submissionState by recordDraftViewModel.submissionState.collectAsStateWithLifecycle()
    val siteStatus by homeViewModel.siteStatus.collectAsStateWithLifecycle()
    val submitRecord =
        rememberRecordSubmitter(
            recordDraftViewModel = recordDraftViewModel,
            onSaveSuccess = onSaveSuccess,
            onSaveFailure = onSaveFailure,
        )

    TemperatureRecordScreen(
        onMenuClick = onMenuClick,
        onNotificationClick = onNotificationClick,
        currentTemperature = temperatureValueText(siteStatus.temperature),
        humidity = humidityValueText(siteStatus.humidity),
        feelsLikeTemperature = temperatureValueText(siteStatus.apparentTemperature),
        temperatureText = draftState.temperatureText,
        humidityText = draftState.humidityText,
        isManualInputEnabled = draftState.isManualInputEnabled,
        selectedFieldPhotoCount = draftState.fieldPhotoUris.size,
        isSaveEnabled =
            draftState.canSubmitTemperatureRecord &&
                draftState.fieldPhotoUris.isNotEmpty() &&
                submissionState.canAttemptSubmission,
        onTemperatureChange = recordDraftViewModel::updateTemperatureText,
        onHumidityChange = recordDraftViewModel::updateHumidityText,
        onManualInputChange = recordDraftViewModel::updateManualInputEnabled,
        onFieldPhotoClick = onFieldPhotoClick,
        onSaveClick = submitRecord,
    )
}
