package com.nativelap.heartguard.view.screen.photo

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.record.model.MAX_RECORD_PHOTO_COUNT
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.BottomActionBar
import com.nativelap.heartguard.view.component.HeartGuardHeader
import com.nativelap.heartguard.view.component.photo.PhotoMemoField
import com.nativelap.heartguard.view.component.photo.PhotoRetakeButton
import com.nativelap.heartguard.view.component.photo.PhotoScreenIntro
import com.nativelap.heartguard.view.component.photo.PhotoSelectionCard
import com.nativelap.heartguard.view.component.photo.SelectedPhotoGrid
import com.nativelap.heartguard.view.component.temperature.RecordSaveButton

/** 작업 전·중 사진 선택과 메모 입력을 Figma 화면 흐름으로 조합한다. */
@Composable
fun WorkPhotoScreen(
    memo: String,
    selectedPhotoCount: Int,
    selectedPhotoUris: List<Uri>,
    onCaptureClick: () -> Unit,
    onRemovePhoto: (Uri) -> Unit,
    onRetakeClick: () -> Unit,
    onMemoChange: (String) -> Unit,
    onUploadClick: () -> Unit,
    isSaveEnabled: Boolean,
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.extraColors.pageBackground,
        bottomBar = {
            BottomActionBar {
                RecordSaveButton(
                    title = stringResource(R.string.photo_upload),
                    onClick = onUploadClick,
                    enabled = isSaveEnabled,
                )
            }
        },
    ) { innerPadding ->
        // 헤더는 자체 여백(HeartGuardHeader의 HeaderHorizontal)으로 좌우 아이콘 위치를 관리하므로,
        // 화면 전체에 가로 패딩을 주지 않고 헤더 아래 콘텐츠에만 별도로 적용한다.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier =
                    Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Section),
            ) {
                HeartGuardHeader(
                    title = stringResource(R.string.brand_name),
                    menuPainter = painterResource(R.drawable.menu_hamburger),
                    notificationPainter = painterResource(R.drawable.notification_bell),
                    onMenuClick = onMenuClick,
                    onNotificationClick = onNotificationClick,
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Section),
                ) {
                    PhotoScreenIntro(
                        title = stringResource(R.string.photo_work_screen_title),
                        description = stringResource(R.string.photo_work_screen_description),
                        modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordTitleHorizontal),
                    )

                    PhotoSelectionCard(
                        title = stringResource(R.string.photo_selection_action_title),
                        description =
                            if (selectedPhotoCount == 0) {
                                stringResource(R.string.photo_selection_empty)
                            } else {
                                stringResource(R.string.photo_selected_count, selectedPhotoCount)
                            },
                        cameraPainter = painterResource(R.drawable.record_camera),
                        cameraContentDescription = stringResource(R.string.photo_capture),
                        onClick = onCaptureClick,
                        isEnabled = selectedPhotoCount < MAX_RECORD_PHOTO_COUNT,
                        modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordPhotoCardHorizontal),
                    )

                    SelectedPhotoGrid(
                        photoUris = selectedPhotoUris,
                        onRemovePhoto = onRemovePhoto,
                        modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordPhotoCardHorizontal),
                    )

                    if (selectedPhotoCount > 0) {
                        // Figma 14 "다시하기" 카드처럼 사진 카드와 같은 좌우 여백 안에 둔다.
                        PhotoRetakeButton(
                            title = stringResource(R.string.photo_retake),
                            onClick = onRetakeClick,
                            modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordPhotoCardHorizontal),
                        )
                    }

                    PhotoMemoField(
                        label = stringResource(R.string.photo_memo),
                        text = memo,
                        onTextChange = onMemoChange,
                        placeholder = stringResource(R.string.photo_work_memo_hint),
                        isOptional = true,
                        contentHorizontalPadding = HeartGuardSpacing.RecordFieldInset,
                        modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordFieldHorizontal),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun WorkPhotoScreenPreview() {
    HeartGuardTheme {
        WorkPhotoScreen(
            memo = "",
            selectedPhotoCount = 0,
            selectedPhotoUris = emptyList(),
            onCaptureClick = {},
            onRemovePhoto = {},
            onRetakeClick = {},
            onMemoChange = {},
            onUploadClick = {},
            isSaveEnabled = true,
        )
    }
}
