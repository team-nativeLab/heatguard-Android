package com.nativelap.heartguard.view.screen.photo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.HeartGuardHeader
import com.nativelap.heartguard.view.component.photo.PhotoMemoField
import com.nativelap.heartguard.view.component.photo.PhotoRetakeButton
import com.nativelap.heartguard.view.component.photo.PhotoSelectionCard
import com.nativelap.heartguard.view.component.photo.PhotoScreenIntro
import com.nativelap.heartguard.view.component.photo.PhotoUploadButton

/** 작업 전·중 사진 선택과 메모 입력을 Figma 화면 흐름으로 조합한다. */
@Composable
fun WorkPhotoScreen(
    memo: String,
    selectedPhotoCount: Int,
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
                .padding(horizontal = HeartGuardSpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Section),
        ) {
            HeartGuardHeader(
                title = stringResource(R.string.brand_name),
                menuPainter = painterResource(R.drawable.menu_hamburger),
                notificationPainter = painterResource(R.drawable.notification_bell),
                onMenuClick = {},
                onNotificationClick = {},
            )

            PhotoScreenIntro(
                title = stringResource(R.string.photo_work_screen_title),
                // TODO: Figma "11_작업 사진 촬영" 프레임 기준 문구다.
                // strings.xml의 photo_work_screen_description 값과 달라 우선 하드코딩한다.
                // 디자인 문구가 확정되면 strings.xml을 갱신해야 한다.
                description = "작업 현장과 보호조치를 확인 할 수 있는 사진을 촬영해 주세요",
            )

            PhotoSelectionCard(
                title = stringResource(R.string.photo_take_instruction),
                description = if (selectedPhotoCount == 0) {
                    // TODO: Figma 박스 안내 문구는 "1 ~ 2장 선택 가능"이다.
                    // strings.xml의 photo_work_instruction 값과 달라 우선 하드코딩한다.
                    "1~2장 선택 가능"
                } else {
                    stringResource(R.string.photo_selected_count, selectedPhotoCount)
                },
                cameraPainter = painterResource(R.drawable.record_camera),
                cameraContentDescription = stringResource(R.string.photo_capture),
                onClick = onCaptureClick,
            )

            if (selectedPhotoCount > 0) {
                PhotoRetakeButton(
                    title = stringResource(R.string.photo_retake),
                    onClick = onRetakeClick,
                )
            }

            PhotoMemoField(
                label = stringResource(R.string.photo_memo),
                text = memo,
                onTextChange = onMemoChange,
                // TODO: Figma 작업 사진 화면 전용 placeholder다. strings.xml에 아직 없어 하드코딩한다.
                placeholder = "작업 전·중 특이사항이 있다면 입력해주세요",
                isOptional = true,
            )

            PhotoUploadButton(
                title = stringResource(R.string.photo_upload),
                onClick = onUploadClick,
                isEnabled = selectedPhotoCount > 0,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 933)
@Composable
private fun WorkPhotoScreenPreview() {
    HeartGuardTheme {
        WorkPhotoScreen(
            memo = "",
            selectedPhotoCount = 0,
            onCaptureClick = {},
            onRetakeClick = {},
            onMemoChange = {},
            onUploadClick = {},
        )
    }
}
