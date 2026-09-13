package com.nativelap.heartguard.view.screen.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.feedback.ConfirmDialogSurface
import com.nativelap.heartguard.view.component.feedback.DialogActionButton
import com.nativelap.heartguard.view.component.photo.UnsavedRecordCard

/** 저장 전 기록과 확인 알림을 정적인 오버레이 화면으로 조합한다. */
@Composable
fun SaveConfirmationScreen(
    onDismissClick: () -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(HeartGuardSpacing.ScreenHorizontal),
            ) {
                UnsavedRecordCard(
                    title = stringResource(R.string.save_confirmation_unsaved_title),
                    description = stringResource(R.string.save_confirmation_unsaved_description),
                    onDismiss = onDismissClick,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = CONFIRMATION_SCRIM_ALPHA)),
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
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
    }
}

private const val CONFIRMATION_SCRIM_ALPHA = 0.32f
private val CONFIRMATION_DIALOG_MAX_WIDTH = 320.dp

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun SaveConfirmationScreenPreview() {
    HeartGuardTheme {
        SaveConfirmationScreen(
            onDismissClick = {},
            onConfirmClick = {},
        )
    }
}
