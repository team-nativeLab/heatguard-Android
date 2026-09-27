package com.nativelap.heartguard.view.component.account

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 회원탈퇴 화면 상단의 가운데 제목과, [onBackClick]이 있으면 왼쪽 뒤로 가기 버튼을 보여준다. */
@Composable
fun WithdrawTopBar(
    title: String,
    modifier: Modifier = Modifier,
    backContentDescription: String? = null,
    onBackClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HeartGuardComponentSize.TouchTarget)
            .padding(horizontal = HeartGuardSpacing.Compact),
        contentAlignment = Alignment.Center,
    ) {
        if (onBackClick != null) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = backContentDescription,
                    modifier = Modifier.size(HeartGuardIconSize.HeaderAction),
                    tint = MaterialTheme.extraColors.strongText,
                )
            }
        }

        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = HeartGuardFontSize.PageTitle,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun WithdrawTopBarPreview() {
    HeartGuardTheme {
        WithdrawTopBar(
            title = "회원탈퇴",
            backContentDescription = "뒤로 가기",
            onBackClick = {},
        )
    }
}
