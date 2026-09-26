package com.nativelap.heartguard.view.route.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.view.component.RecordType
import com.nativelap.heartguard.view.screen.photo.FieldPhotoScreen
import com.nativelap.heartguard.view.screen.photo.RestPhotoScreen
import com.nativelap.heartguard.view.screen.photo.WorkPhotoScreen
import com.nativelap.heartguard.viewmodel.record.RecordDraftViewModel

/** 현장 사진 선택 상태를 [recordDraftViewModel]과 공유해 저장 전 확인 화면까지 값이 유지되게 한다. */
@Composable
internal fun HeartGuardFieldPhotoRoute(
    recordDraftViewModel: RecordDraftViewModel,
    onSaveClick: () -> Unit,
    onCameraClick: (RecordType) -> Unit,
) {
    val draftState by recordDraftViewModel.uiState.collectAsStateWithLifecycle()

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
            currentTemperature = "47.5°C",
            humidity = "55%",
            feelsLikeTemperature = "40.5°C",
            selectedPhotoCount = selectedPhotoUris.size,
            selectedPhotoUris = selectedPhotoUris,
            showTemperatureSaveError = !draftState.isTemperatureSaved,
            onCaptureClick = onAddPhotoClick,
            onRemovePhoto = onRemovePhoto,
            onSaveClick = onSaveClick,
        )
    }
}

/** 작업 사진의 선택 상태와 메모를 [recordDraftViewModel]과 공유해 저장 완료 Navigation을 연결한다. */
@Composable
internal fun HeartGuardWorkPhotoRoute(
    recordDraftViewModel: RecordDraftViewModel,
    onUploadClick: () -> Unit,
    onCameraClick: (RecordType) -> Unit,
) {
    val draftState by recordDraftViewModel.uiState.collectAsStateWithLifecycle()

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
            onUploadClick = onUploadClick,
        )
    }
}

/** 휴식 사진의 시간·선택 상태·메모를 [recordDraftViewModel]과 공유해 저장 완료 Navigation을 연결한다. */
@Composable
internal fun HeartGuardRestPhotoRoute(
    recordDraftViewModel: RecordDraftViewModel,
    onUploadClick: () -> Unit,
    onCameraClick: (RecordType) -> Unit,
) {
    val draftState by recordDraftViewModel.uiState.collectAsStateWithLifecycle()

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
            selectedRestTime = if (draftState.isAlternateRestTimeSelected) {
                stringResource(R.string.photo_rest_selected_time_alternate)
            } else {
                stringResource(R.string.photo_rest_selected_time)
            },
            selectedPhotoCount = selectedPhotoUris.size,
            selectedPhotoUris = selectedPhotoUris,
            onRestTimeClick = recordDraftViewModel::toggleAlternateRestTime,
            onCaptureClick = onAddPhotoClick,
            onRemovePhoto = onRemovePhoto,
            onRetakeClick = onClearPhotos,
            onMemoChange = recordDraftViewModel::updateRestMemo,
            onUploadClick = onUploadClick,
        )
    }
}
