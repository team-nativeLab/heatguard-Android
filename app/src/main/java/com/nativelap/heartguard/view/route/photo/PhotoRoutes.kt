package com.nativelap.heartguard.view.route.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nativelap.heartguard.view.component.RecordType
import com.nativelap.heartguard.view.component.valueOrEmptyText
import com.nativelap.heartguard.view.route.record.rememberRecordSubmitter
import com.nativelap.heartguard.view.screen.photo.FieldPhotoScreen
import com.nativelap.heartguard.view.screen.photo.RestPhotoScreen
import com.nativelap.heartguard.view.screen.photo.WorkPhotoScreen
import com.nativelap.heartguard.viewmodel.record.RecordDraftViewModel
import com.nativelap.heartguard.viewmodel.record.RecordSubmissionState

/** 온도계 기록의 현장 사진 화면(Figma 14/15)이다. 사진 선택 상태는 [recordDraftViewModel]과 공유한다.
 * 사진 없이 "저장"을 누르면 저장하지 않고 15 상태(온도계가 아직 저장이 안되었어요)를 보여주며, 사진을 고르면 그 상태를 해제한다.
 * 사진이 있으면 바로 기록을 등록하고 결과에 따라 [onSaveSuccess]/[onSaveFailure]로 이동한다. */
@Composable
internal fun HeartGuardFieldPhotoRoute(
    recordDraftViewModel: RecordDraftViewModel,
    onCameraClick: (RecordType) -> Unit,
    onSaveSuccess: () -> Unit,
    onSaveFailure: () -> Unit,
) {
    val draftState by recordDraftViewModel.uiState.collectAsStateWithLifecycle()
    val submissionState by recordDraftViewModel.submissionState.collectAsStateWithLifecycle()
    val submitRecord = rememberRecordSubmitter(
        recordDraftViewModel = recordDraftViewModel,
        onSaveSuccess = onSaveSuccess,
        onSaveFailure = onSaveFailure,
    )
    var showSaveError by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(draftState.fieldPhotoUris.isNotEmpty()) {
        if (draftState.fieldPhotoUris.isNotEmpty()) {
            showSaveError = false
        }
    }

    PhotoSelectionFlow(
        selectedPhotoUris = draftState.fieldPhotoUris,
        onPhotoAdded = { photoUri ->
            recordDraftViewModel.addPhoto(RecordType.TEMPERATURE, photoUri)
        },
        onRemovePhoto = { photoUri ->
            recordDraftViewModel.removePhoto(RecordType.TEMPERATURE, photoUri)
        },
        onClearPhotos = { recordDraftViewModel.clearPhotos(RecordType.TEMPERATURE) },
        onCameraClick = { onCameraClick(RecordType.TEMPERATURE) },
    ) { selectedPhotoUris, onAddPhotoClick, onRemovePhoto, _ ->
        FieldPhotoScreen(
            manualTemperature = valueOrEmptyText(
                draftState.temperatureText.takeIf { draftState.isManualInputEnabled && it.isNotBlank() },
            ),
            manualHumidity = valueOrEmptyText(
                draftState.humidityText.takeIf { draftState.isManualInputEnabled && it.isNotBlank() },
            ),
            selectedPhotoUris = selectedPhotoUris,
            showSaveError = showSaveError,
            isSaveEnabled = submissionState !is RecordSubmissionState.Submitting,
            onCaptureClick = onAddPhotoClick,
            onRemovePhoto = onRemovePhoto,
            onSaveClick = {
                if (selectedPhotoUris.isEmpty()) {
                    showSaveError = true
                } else {
                    submitRecord()
                }
            },
        )
    }
}

/** 작업 사진 화면(Figma 11)이다. 사진·메모를 [recordDraftViewModel]과 공유하고,
 * "기록 저장"은 확인 화면 없이 바로 기록을 등록해 결과에 따라 [onSaveSuccess]/[onSaveFailure]로 이동한다. */
@Composable
internal fun HeartGuardWorkPhotoRoute(
    recordDraftViewModel: RecordDraftViewModel,
    onCameraClick: (RecordType) -> Unit,
    onSaveSuccess: () -> Unit,
    onSaveFailure: () -> Unit,
) {
    val draftState by recordDraftViewModel.uiState.collectAsStateWithLifecycle()
    val submissionState by recordDraftViewModel.submissionState.collectAsStateWithLifecycle()
    val submitRecord = rememberRecordSubmitter(
        recordDraftViewModel = recordDraftViewModel,
        onSaveSuccess = onSaveSuccess,
        onSaveFailure = onSaveFailure,
    )

    PhotoSelectionFlow(
        selectedPhotoUris = draftState.workPhotoUris,
        onPhotoAdded = { photoUri ->
            recordDraftViewModel.addPhoto(RecordType.WORK, photoUri)
        },
        onRemovePhoto = { photoUri ->
            recordDraftViewModel.removePhoto(RecordType.WORK, photoUri)
        },
        onClearPhotos = { recordDraftViewModel.clearPhotos(RecordType.WORK) },
        onCameraClick = { onCameraClick(RecordType.WORK) },
    ) { selectedPhotoUris, onAddPhotoClick, onRemovePhoto, onClearPhotos ->
        WorkPhotoScreen(
            memo = draftState.workMemo,
            selectedPhotoCount = selectedPhotoUris.size,
            selectedPhotoUris = selectedPhotoUris,
            onCaptureClick = onAddPhotoClick,
            onRemovePhoto = onRemovePhoto,
            onRetakeClick = onClearPhotos,
            onMemoChange = recordDraftViewModel::updateWorkMemo,
            onUploadClick = submitRecord,
            isSaveEnabled = selectedPhotoUris.isNotEmpty() &&
                submissionState !is RecordSubmissionState.Submitting,
        )
    }
}

/** 휴식 사진 화면(Figma 12)이다. 사진·메모를 [recordDraftViewModel]과 공유하고,
 * "기록 저장"은 확인 화면 없이 바로 기록을 등록해 결과에 따라 [onSaveSuccess]/[onSaveFailure]로 이동한다. */
@Composable
internal fun HeartGuardRestPhotoRoute(
    recordDraftViewModel: RecordDraftViewModel,
    onCameraClick: (RecordType) -> Unit,
    onSaveSuccess: () -> Unit,
    onSaveFailure: () -> Unit,
) {
    val draftState by recordDraftViewModel.uiState.collectAsStateWithLifecycle()
    val submissionState by recordDraftViewModel.submissionState.collectAsStateWithLifecycle()
    val submitRecord = rememberRecordSubmitter(
        recordDraftViewModel = recordDraftViewModel,
        onSaveSuccess = onSaveSuccess,
        onSaveFailure = onSaveFailure,
    )

    PhotoSelectionFlow(
        selectedPhotoUris = draftState.restPhotoUris,
        onPhotoAdded = { photoUri ->
            recordDraftViewModel.addPhoto(RecordType.REST, photoUri)
        },
        onRemovePhoto = { photoUri ->
            recordDraftViewModel.removePhoto(RecordType.REST, photoUri)
        },
        onClearPhotos = { recordDraftViewModel.clearPhotos(RecordType.REST) },
        onCameraClick = { onCameraClick(RecordType.REST) },
    ) { selectedPhotoUris, onAddPhotoClick, onRemovePhoto, onClearPhotos ->
        RestPhotoScreen(
            memo = draftState.restMemo,
            selectedPhotoCount = selectedPhotoUris.size,
            selectedPhotoUris = selectedPhotoUris,
            onCaptureClick = onAddPhotoClick,
            onRemovePhoto = onRemovePhoto,
            onRetakeClick = onClearPhotos,
            onMemoChange = recordDraftViewModel::updateRestMemo,
            onUploadClick = submitRecord,
            isSaveEnabled = selectedPhotoUris.isNotEmpty() &&
                submissionState !is RecordSubmissionState.Submitting,
        )
    }
}
