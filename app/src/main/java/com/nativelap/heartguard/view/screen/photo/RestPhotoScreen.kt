package com.nativelap.heartguard.view.screen.photo

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.photo.RestTimeCard

/** 휴식 시간·사진 선택·메모 입력을 Figma 휴식 사진 화면으로 조합한다. */
@Composable
fun RestPhotoScreen(
    memo: String,
    selectedPhotoCount: Int,
    selectedPhotoUris: List<Uri>,
    onCaptureClick: () -> Unit,
    onRemovePhoto: (Uri) -> Unit,
    onRetakeClick: () -> Unit,
    onMemoChange: (String) -> Unit,
    selectedRestTime: String,
    onRestTimeClick: () -> Unit,
    hasRestTimeError: Boolean,
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
        titleResource = R.string.photo_rest_screen_title,
        descriptionResource = R.string.photo_rest_screen_description,
        memoHintResource = R.string.photo_rest_memo_hint,
        extraContent = {
            RestTimeCard(
                title = stringResource(R.string.photo_rest_time),
                selectedTime = selectedRestTime,
                onClick = onRestTimeClick,
                contentHorizontalPadding = HeartGuardSpacing.RecordFieldInset,
                errorMessage = if (hasRestTimeError) stringResource(R.string.photo_rest_time_required) else null,
                modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordFieldHorizontal),
            )
        },
    )
}

@Preview(showBackground = true, widthDp = 402, heightDp = 933)
@Composable
private fun RestPhotoScreenPreview() {
    HeartGuardTheme {
        RestPhotoScreen(
            onBackClick = {},
            memo = "",
            selectedPhotoCount = 0,
            selectedPhotoUris = emptyList(),
            onCaptureClick = {},
            onRemovePhoto = {},
            onRetakeClick = {},
            onMemoChange = {},
            selectedRestTime = "13 : 00 ~ 13 : 30",
            onRestTimeClick = {},
            hasRestTimeError = false,
            onUploadClick = {},
            isSaveEnabled = true,
        )
    }
}
