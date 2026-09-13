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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.photo.PhotoCaptureButton
import com.nativelap.heartguard.view.component.photo.PhotoSelectionCard

/** 현장 사진 촬영 전 상태를 카메라 선택 Component 조합으로 구성한다. */
@Composable
fun FieldPhotoScreen(
    onCaptureClick: () -> Unit,
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
            PhotoSelectionCard(
                title = stringResource(R.string.photo_take_instruction),
                description = stringResource(R.string.photo_field_instruction),
                cameraPainter = painterResource(R.drawable.record_camera),
                cameraContentDescription = stringResource(R.string.photo_capture),
                onClick = onCaptureClick,
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
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun FieldPhotoScreenPreview() {
    HeartGuardTheme {
        FieldPhotoScreen(onCaptureClick = {})
    }
}
