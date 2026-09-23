package com.nativelap.heartguard.view.route.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.view.screen.photo.FieldPhotoScreen
import com.nativelap.heartguard.view.screen.photo.RestPhotoScreen
import com.nativelap.heartguard.view.screen.photo.WorkPhotoScreen

/** 현장 사진 선택 상태를 보유하고 저장 완료 Navigation callback을 연결한다. */
@Composable
internal fun HeartGuardFieldPhotoRoute(
    onSaveClick: () -> Unit,
) {
    PhotoSelectionFlow { selectedPhotoUris, onAddPhotoClick, onRemovePhoto, _ ->
        FieldPhotoScreen(
            currentTemperature = "47.5°C",
            humidity = "55%",
            feelsLikeTemperature = "40.5°C",
            selectedPhotoCount = selectedPhotoUris.size,
            selectedPhotoUris = selectedPhotoUris,
            showTemperatureSaveError = false,
            onCaptureClick = onAddPhotoClick,
            onRemovePhoto = onRemovePhoto,
            onSaveClick = onSaveClick,
        )
    }
}

/** 작업 사진의 로컬 선택 개수와 메모 상태를 관리하고 저장 Navigation을 연결한다. */
@Composable
internal fun HeartGuardWorkPhotoRoute(
    onUploadClick: () -> Unit,
) {
    var memo by rememberSaveable {
        mutableStateOf("")
    }
    PhotoSelectionFlow { selectedPhotoUris, onAddPhotoClick, onRemovePhoto, onClearPhotos ->
        WorkPhotoScreen(
            memo = memo,
            selectedPhotoCount = selectedPhotoUris.size,
            selectedPhotoUris = selectedPhotoUris,
            onCaptureClick = onAddPhotoClick,
            onRemovePhoto = onRemovePhoto,
            onRetakeClick = onClearPhotos,
            onMemoChange = { memo = it },
            onUploadClick = onUploadClick,
        )
    }
}

/** 휴식 사진의 시간·선택 개수·메모를 관리하고 저장 Navigation을 연결한다. */
@Composable
internal fun HeartGuardRestPhotoRoute(
    onUploadClick: () -> Unit,
) {
    var memo by rememberSaveable {
        mutableStateOf("")
    }
    var isAlternateRestTimeSelected by rememberSaveable {
        mutableStateOf(false)
    }

    PhotoSelectionFlow { selectedPhotoUris, onAddPhotoClick, onRemovePhoto, onClearPhotos ->
        RestPhotoScreen(
            memo = memo,
            selectedRestTime = if (isAlternateRestTimeSelected) {
                stringResource(R.string.photo_rest_selected_time_alternate)
            } else {
                stringResource(R.string.photo_rest_selected_time)
            },
            selectedPhotoCount = selectedPhotoUris.size,
            selectedPhotoUris = selectedPhotoUris,
            onRestTimeClick = {
                isAlternateRestTimeSelected = !isAlternateRestTimeSelected
            },
            onCaptureClick = onAddPhotoClick,
            onRemovePhoto = onRemovePhoto,
            onRetakeClick = onClearPhotos,
            onMemoChange = { memo = it },
            onUploadClick = onUploadClick,
        )
    }
}
