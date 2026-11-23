package com.nativelap.heartguard.view.screen.photo

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardTheme

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
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onNotificationClick: () -> Unit = {},
) {
    RecordPhotoScreen(
        memo = memo,
        selectedPhotoCount = selectedPhotoCount,
        selectedPhotoUris = selectedPhotoUris,
        onCaptureClick = onCaptureClick,
        onRemovePhoto = onRemovePhoto,
        onRetakeClick = onRetakeClick,
        onMemoChange = onMemoChange,
        onUploadClick = onUploadClick,
        isSaveEnabled = isSaveEnabled,
        onBackClick = onBackClick,
        modifier = modifier,
        onNotificationClick = onNotificationClick,
        titleResource = R.string.photo_work_screen_title,
        descriptionResource = R.string.photo_work_screen_description,
        memoHintResource = R.string.photo_work_memo_hint,
    )
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun WorkPhotoScreenPreview() {
    HeartGuardTheme {
        WorkPhotoScreen(
            onBackClick = {},
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
