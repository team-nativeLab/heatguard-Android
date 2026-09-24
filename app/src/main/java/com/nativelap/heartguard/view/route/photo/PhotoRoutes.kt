package com.nativelap.heartguard.view.route.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.view.screen.photo.FieldPhotoScreen
import com.nativelap.heartguard.view.screen.photo.RestPhotoScreen
import com.nativelap.heartguard.view.screen.photo.WorkPhotoScreen
import com.nativelap.heartguard.viewmodel.record.RecordDraftViewModel

/** 현장 사진 선택 상태를 [recordDraftViewModel]과 공유해 저장 전 확인 화면까지 값이 유지되게 한다. */
@Composable
internal fun HeartGuardFieldPhotoRoute(
    recordDraftViewModel: RecordDraftViewModel,
    onSaveClick: () -> Unit,
) {
    // TODO: androidx.lifecycle:lifecycle-runtime-compose 도입이 확정되면 collectAsStateWithLifecycle로 교체한다.
    val draftState by recordDraftViewModel.uiState.collectAsState()

    PhotoSelectionFlow(
        initialPhotoUris = draftState.fieldPhotoUris,
        onPhotosChanged = recordDraftViewModel::updateFieldPhotos,
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
) {
    val draftState by recordDraftViewModel.uiState.collectAsState()

    PhotoSelectionFlow(
        initialPhotoUris = draftState.workPhotoUris,
        onPhotosChanged = recordDraftViewModel::updateWorkPhotos,
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
) {
    val draftState by recordDraftViewModel.uiState.collectAsState()

    PhotoSelectionFlow(
        initialPhotoUris = draftState.restPhotoUris,
        onPhotosChanged = recordDraftViewModel::updateRestPhotos,
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
