package com.nativelap.heartguard.view.route.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
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
    var selectedPhotoCount by rememberSaveable {
        mutableIntStateOf(0)
    }
    FieldPhotoScreen(
        currentTemperature = "47.5°C",
        humidity = "55%",
        feelsLikeTemperature = "40.5°C",
        selectedPhotoCount = selectedPhotoCount,
        showTemperatureSaveError = false,
        onCaptureClick = {
            if (selectedPhotoCount < 2) {
                selectedPhotoCount += 1
            }
        },
        onSaveClick = onSaveClick,
    )
}

/** 작업 사진의 로컬 선택 개수와 메모 상태를 관리하고 저장 Navigation을 연결한다. */
@Composable
internal fun HeartGuardWorkPhotoRoute(
    onUploadClick: () -> Unit,
) {
    var memo by rememberSaveable {
        mutableStateOf("")
    }
    var selectedPhotoCount by rememberSaveable {
        mutableIntStateOf(0)
    }

    WorkPhotoScreen(
        memo = memo,
        selectedPhotoCount = selectedPhotoCount,
        onCaptureClick = {
            if (selectedPhotoCount < 2) {
                selectedPhotoCount += 1
            }
        },
        onRetakeClick = { selectedPhotoCount = 0 },
        onMemoChange = { memo = it },
        onUploadClick = onUploadClick,
    )
}

/** 휴식 사진의 시간·선택 개수·메모를 관리하고 저장 Navigation을 연결한다. */
@Composable
internal fun HeartGuardRestPhotoRoute(
    onUploadClick: () -> Unit,
) {
    var memo by rememberSaveable {
        mutableStateOf("")
    }
    var selectedPhotoCount by rememberSaveable {
        mutableIntStateOf(0)
    }
    var isAlternateRestTimeSelected by rememberSaveable {
        mutableStateOf(false)
    }

    RestPhotoScreen(
        memo = memo,
        selectedRestTime = if (isAlternateRestTimeSelected) {
            stringResource(R.string.photo_rest_selected_time_alternate)
        } else {
            stringResource(R.string.photo_rest_selected_time)
        },
        selectedPhotoCount = selectedPhotoCount,
        onRestTimeClick = {
            isAlternateRestTimeSelected = !isAlternateRestTimeSelected
        },
        onCaptureClick = {
            if (selectedPhotoCount < 2) {
                selectedPhotoCount += 1
            }
        },
        onRetakeClick = { selectedPhotoCount = 0 },
        onMemoChange = { memo = it },
        onUploadClick = onUploadClick,
    )
}
