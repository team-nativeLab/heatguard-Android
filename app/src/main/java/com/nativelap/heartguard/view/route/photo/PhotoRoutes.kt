package com.nativelap.heartguard.view.route.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.view.screen.photo.FieldPhotoScreen
import com.nativelap.heartguard.view.screen.photo.RestPhotoScreen
import com.nativelap.heartguard.view.screen.photo.WorkPhotoScreen

/** 현장 사진 촬영 전 Screen을 표시하는 Route adapter이다. */
@Composable
internal fun HeartGuardFieldPhotoRoute() {
    FieldPhotoScreen(onCaptureClick = {})
}

/** 작업 사진 메모 상태를 관리하고 업로드 Navigation callback을 전달한다. */
@Composable
internal fun HeartGuardWorkPhotoRoute(
    onUploadClick: () -> Unit,
) {
    var memo by rememberSaveable {
        mutableStateOf("")
    }

    WorkPhotoScreen(
        memo = memo,
        photoPainter = null,
        onCaptureClick = {},
        onRetakeClick = {},
        onMemoChange = { memo = it },
        onUploadClick = onUploadClick,
    )
}

/** 휴식 사진 메모 상태와 휴식 시간 표시를 관리하는 Route adapter이다. */
@Composable
internal fun HeartGuardRestPhotoRoute(
    onUploadClick: () -> Unit,
) {
    var memo by rememberSaveable {
        mutableStateOf("")
    }

    RestPhotoScreen(
        memo = memo,
        selectedRestTime = stringResource(R.string.photo_rest_selected_time),
        photoPainter = null,
        onRestTimeClick = {},
        onCaptureClick = {},
        onRetakeClick = {},
        onMemoChange = { memo = it },
        onUploadClick = onUploadClick,
    )
}
