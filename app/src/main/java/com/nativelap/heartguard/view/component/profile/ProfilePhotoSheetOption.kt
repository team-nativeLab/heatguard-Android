package com.nativelap.heartguard.view.component.profile

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.extraColors

/** 프로필 사진 변경 시트의 옵션 한 행이다. 행 전체를 누를 수 있고, 아이콘은 벡터 파일의 원래 색을 그대로 쓴다. */
@Composable
fun ProfilePhotoSheetOption(
    label: String,
    @DrawableRes iconResId: Int,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
) {
    val labelColor =
        if (isDestructive) {
            MaterialTheme.extraColors.sheetDestructiveText
        } else {
            MaterialTheme.extraColors.strongText
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(HeartGuardComponentSize.SheetOptionHeight)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = HeartGuardSpacing.Card),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(iconResId),
            contentDescription = null,
            modifier = Modifier.size(HeartGuardIconSize.ProfilePhotoSheetOption),
        )

        Spacer(modifier = Modifier.width(HeartGuardSpacing.Item))

        Text(
            text = label,
            color = labelColor,
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    fontSize = HeartGuardFontSize.ProfilePhotoSheetOption,
                    fontWeight = FontWeight.Medium,
                ),
        )
    }
}
