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
import com.nativelap.heartguard.view.component.photo.PhotoUploadButton

/** 작업 사진 기록의 촬영 전·촬영 후 상태를 하나의 조합 화면으로 표현한다. */
@Composable
fun WorkPhotoScreen(
    memo: String,
    photoPainter: Painter?,
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
            if (photoPainter == null) {
                PhotoSelectionCard(
                    title = stringResource(R.string.photo_take_instruction),
                    description = stringResource(R.string.photo_work_instruction),
                    cameraPainter = painterResource(R.drawable.record_camera),
                    cameraContentDescription = stringResource(R.string.photo_capture),
                    onClick = onCaptureClick,
                    selectedPhotoCountLabel = stringResource(R.string.photo_count_empty),
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
                placeholder = stringResource(R.string.photo_memo_hint),
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

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun WorkPhotoScreenPreview() {
    HeartGuardTheme {
        WorkPhotoScreen(
            memo = "",
            photoPainter = null,
            onCaptureClick = {},
            onRetakeClick = {},
            onMemoChange = {},
            onUploadClick = {},
        )
    }
}
