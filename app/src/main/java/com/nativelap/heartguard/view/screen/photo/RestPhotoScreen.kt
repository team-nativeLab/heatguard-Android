package com.nativelap.heartguard.view.screen.photo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.photo.PhotoCaptureButton
import com.nativelap.heartguard.view.component.photo.PhotoMemoField
import com.nativelap.heartguard.view.component.photo.PhotoPreview
import com.nativelap.heartguard.view.component.photo.PhotoRetakeButton
import com.nativelap.heartguard.view.component.photo.PhotoSelectionCard
import com.nativelap.heartguard.view.component.photo.PhotoScreenIntro
import com.nativelap.heartguard.view.component.photo.PhotoUploadButton
import com.nativelap.heartguard.view.component.photo.RestTimeCard

/** 휴식 시간 선택과 휴식 사진 기록 UI를 기존 Photo Component로 조합한다. */
@Composable
fun RestPhotoScreen(
    memo: String,
    selectedRestTime: String,
    photoPainter: Painter?,
    onRestTimeClick: () -> Unit,
    onCaptureClick: () -> Unit,
    onRetakeClick: () -> Unit,
    onMemoChange: (String) -> Unit,
    onUploadClick: () -> Unit,
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
                .padding(innerPadding)
                .padding(HeartGuardSpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Section),
        ) {
            PhotoScreenIntro(
                title = stringResource(R.string.photo_rest_screen_title),
                description = stringResource(R.string.photo_rest_screen_description),
            )

            RestTimeCard(
                title = stringResource(R.string.photo_rest_time),
                selectedTime = selectedRestTime,
                onClick = onRestTimeClick,
            )

            if (photoPainter == null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
                ) {
                    Text(
                        text = stringResource(R.string.photo_rest_photo_label),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    PhotoSelectionCard(
                        title = stringResource(R.string.photo_take_instruction),
                        description = stringResource(R.string.photo_rest_instruction),
                        cameraPainter = painterResource(R.drawable.record_camera),
                        onClick = onCaptureClick,
                        cameraContentDescription = stringResource(R.string.photo_capture),
                        selectedPhotoCountLabel = stringResource(R.string.photo_count_rest_empty),
                    )
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        PhotoCaptureButton(
                            cameraPainter = painterResource(R.drawable.record_camera),
                            contentDescription = stringResource(R.string.photo_capture),
                            onClick = onCaptureClick,
                        )
                    }
                }
            } else {
                PhotoPreview(
                    photoPainter = photoPainter,
                    contentDescription = stringResource(R.string.photo_preview),
                )
                PhotoRetakeButton(
                    title = stringResource(R.string.photo_retake),
                    onClick = onRetakeClick,
                )
            }

            PhotoMemoField(
                label = stringResource(R.string.photo_memo),
                text = memo,
                onTextChange = onMemoChange,
                placeholder = stringResource(R.string.photo_rest_memo_hint),
                isOptional = true,
            )

            PhotoUploadButton(
                title = stringResource(R.string.photo_upload),
                onClick = onUploadClick,
                isEnabled = photoPainter != null,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 933)
@Composable
private fun RestPhotoScreenPreview() {
    HeartGuardTheme {
        RestPhotoScreen(
            memo = "",
            selectedRestTime = stringResource(R.string.photo_rest_selected_time),
            photoPainter = null,
            onRestTimeClick = {},
            onCaptureClick = {},
            onRetakeClick = {},
            onMemoChange = {},
            onUploadClick = {},
        )
    }
}
