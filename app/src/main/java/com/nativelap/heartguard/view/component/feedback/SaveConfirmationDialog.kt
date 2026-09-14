package com.nativelap.heartguard.view.component.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing

// 이전 미사용 SaveConfirmationScreen.kt(삭제됨)에만 있던 다이얼로그 최대 너비 스펙을 컴포넌트 자체 책임으로 옮겼다.
private val CONFIRMATION_DIALOG_MAX_WIDTH = 320.dp

/** Navigation 3 DialogScene 안에서 저장 확인과 취소·확인 동작을 제공한다. */
@Composable
fun SaveConfirmationDialog(
    onDismissClick: () -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = CONFIRMATION_DIALOG_MAX_WIDTH),
        shape = RoundedCornerShape(HeartGuardRadius.Card),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            ConfirmDialogSurface(
                title = stringResource(R.string.save_confirmation_title),
                message = stringResource(R.string.save_confirmation_message),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = HeartGuardSpacing.Section,
                        vertical = HeartGuardSpacing.Compact,
                    ),
                horizontalArrangement = Arrangement.End,
            ) {
                DialogActionButton(
                    title = stringResource(R.string.common_cancel),
                    onClick = onDismissClick,
                )
                DialogActionButton(
                    title = stringResource(R.string.common_confirm),
                    onClick = onConfirmClick,
                )
            }
        }
    }
}
