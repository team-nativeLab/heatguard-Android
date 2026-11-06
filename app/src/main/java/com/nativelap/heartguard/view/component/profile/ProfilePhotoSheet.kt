package com.nativelap.heartguard.view.component.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardBorderWidth
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.sheet.HeartGuardFigmaBottomSheet

/** 프로필 사진 변경 시트(Figma 1867:1329)다. 사진 찍기, 앨범에서 선택, 기본 이미지로 변경 옵션과 취소를 둔다.
 * 옵션을 고른 뒤 시트를 닫을지는 호출자가 정한다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePhotoSheet(
    onDismissRequest: () -> Unit,
    onCaptureClick: () -> Unit,
    onAlbumClick: () -> Unit,
    onDefaultPhotoClick: () -> Unit,
) {
    HeartGuardFigmaBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = stringResource(R.string.profile_photo_sheet_title),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.extraColors.strongText,
            style =
                MaterialTheme.typography.titleLarge.copy(
                    fontSize = HeartGuardFontSize.ProfilePhotoSheetTitle,
                    fontWeight = FontWeight.Bold,
                ),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(HeartGuardSpacing.Card))

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(HeartGuardRadius.Card))
                    .background(MaterialTheme.colorScheme.surface),
        ) {
            ProfilePhotoSheetOption(
                label = stringResource(R.string.profile_photo_sheet_capture),
                iconResId = R.drawable.ic_sheet_camera,
                onClick = onCaptureClick,
            )

            HorizontalDivider(
                color = MaterialTheme.extraColors.sheetOptionDivider,
                thickness = HeartGuardBorderWidth.Divider,
            )

            ProfilePhotoSheetOption(
                label = stringResource(R.string.profile_photo_sheet_album),
                iconResId = R.drawable.ic_sheet_album,
                onClick = onAlbumClick,
            )

            HorizontalDivider(
                color = MaterialTheme.extraColors.sheetOptionDivider,
                thickness = HeartGuardBorderWidth.Divider,
            )

            ProfilePhotoSheetOption(
                label = stringResource(R.string.profile_photo_sheet_default),
                iconResId = R.drawable.ic_sheet_delete,
                onClick = onDefaultPhotoClick,
                isDestructive = true,
            )
        }

        Spacer(modifier = Modifier.height(HeartGuardSpacing.Item))

        Button(
            onClick = onDismissRequest,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(HeartGuardComponentSize.SheetCancelHeight),
            shape = RoundedCornerShape(HeartGuardRadius.PrimaryAction),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.extraColors.sheetCancelContainer,
                    contentColor = MaterialTheme.extraColors.strongText,
                ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Text(
                text = stringResource(R.string.common_cancel),
                style =
                    MaterialTheme.typography.bodyLarge.copy(
                        fontSize = HeartGuardFontSize.ProfilePhotoSheetCancel,
                        fontWeight = FontWeight.Medium,
                    ),
            )
        }
    }
}
