package com.nativelap.heartguard.view.screen.photo

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.nativelap.heartguard.view.component.photo.RestTimeCard
import com.nativelap.heartguard.view.component.photo.SelectedPhotoGrid

/** 휴식 시간·사진 선택·메모 입력을 Figma 휴식 사진 화면으로 조합한다. */
@Composable
fun RestPhotoScreen(
    memo: String,
    selectedRestTime: String,
    selectedPhotoCount: Int,
    selectedPhotoUris: List<Uri> = emptyList(),
    onRestTimeClick: () -> Unit,
    onCaptureClick: () -> Unit,
    onRemovePhoto: (Uri) -> Unit = {},
    onRetakeClick: () -> Unit,
    onMemoChange: (String) -> Unit,
    onUploadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        // 헤더는 자체 여백(HeartGuardHeader의 HeaderHorizontal)으로 좌우 아이콘 위치를 관리하므로,
        // 화면 전체에 가로 패딩을 주지 않고 헤더 아래 콘텐츠에만 별도로 적용한다.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Section),
        ) {
            HeartGuardHeader(
                title = stringResource(R.string.brand_name),
                menuPainter = painterResource(R.drawable.menu_hamburger),
                notificationPainter = painterResource(R.drawable.notification_bell),
                onMenuClick = {},
                onNotificationClick = {},
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Section),
            ) {
                PhotoScreenIntro(
                    title = stringResource(R.string.photo_rest_screen_title),
                    description = stringResource(R.string.photo_rest_screen_description),
                    modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordTitleHorizontal),
                )

                // Figma 12_휴식 사진 촬영: 사진 선택 카드가 휴식 시간 카드보다 먼저 온다.
                PhotoSelectionCard(
                    title = stringResource(R.string.photo_selection_action_title),
                    description = if (selectedPhotoCount == 0) {
                        stringResource(R.string.photo_selection_empty)
                    } else {
                        stringResource(R.string.photo_selected_count, selectedPhotoCount)
                    },
                    cameraPainter = painterResource(R.drawable.record_camera),
                    cameraContentDescription = stringResource(R.string.photo_capture),
                    onClick = onCaptureClick,
                    isEnabled = selectedPhotoCount < 2,
                    modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordPhotoCardHorizontal),
                )

                SelectedPhotoGrid(
                    photoUris = selectedPhotoUris,
                    onRemovePhoto = onRemovePhoto,
                    modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordPhotoCardHorizontal),
                )

                if (selectedPhotoCount > 0) {
                    PhotoRetakeButton(
                        title = stringResource(R.string.photo_retake),
                        onClick = onRetakeClick,
                    )
                }

                RestTimeCard(
                    title = stringResource(R.string.photo_rest_time),
                    selectedTime = selectedRestTime,
                    onClick = onRestTimeClick,
                    contentHorizontalPadding = HeartGuardSpacing.RecordFieldInset,
                    modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordFieldHorizontal),
                )

                PhotoMemoField(
                    label = stringResource(R.string.photo_memo),
                    text = memo,
                    onTextChange = onMemoChange,
                    placeholder = stringResource(R.string.photo_rest_memo_hint),
                    isOptional = true,
                    contentHorizontalPadding = HeartGuardSpacing.RecordFieldInset,
                    modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordFieldHorizontal),
                )

                PhotoUploadButton(
                    title = stringResource(R.string.photo_upload),
                    onClick = onUploadClick,
                    modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordContentHorizontal),
                )
            }
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
            selectedPhotoCount = 0,
            selectedPhotoUris = emptyList(),
            onRestTimeClick = {},
            onCaptureClick = {},
            onRetakeClick = {},
            onMemoChange = {},
            onUploadClick = {},
        )
    }
}
