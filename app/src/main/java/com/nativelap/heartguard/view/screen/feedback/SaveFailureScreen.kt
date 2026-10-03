package com.nativelap.heartguard.view.screen.feedback

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.BottomActionBar
import com.nativelap.heartguard.view.component.HeartGuardSheetSurface
import com.nativelap.heartguard.view.component.feedback.RetryButton
import com.nativelap.heartguard.view.component.feedback.SaveDraftExitButton
import com.nativelap.heartguard.view.component.feedback.SaveErrorDetailCard
import com.nativelap.heartguard.view.component.feedback.SaveResultMessage
import com.nativelap.heartguard.view.component.feedback.SaveResultTitle

/** 저장 실패 메시지와 원인·재시도 동작을 바텀시트(Figma 10)로 조합한다. */
@Composable
fun SaveFailureScreen(
    errorDetails: List<String>,
    onRetryClick: () -> Unit,
    onSaveDraftAndExitClick: () -> Unit,
    modifier: Modifier = Modifier,
    isResultUnknown: Boolean = false,
) {
    HeartGuardSheetSurface(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = HeartGuardSpacing.ResultHorizontal),
        ) {
            Spacer(modifier = Modifier.height(HeartGuardSpacing.ResultTitleTop))
            SaveResultTitle(
                title = stringResource(R.string.save_record_title),
            )
            Spacer(modifier = Modifier.height(HeartGuardSpacing.ResultTitleMessageGap))

            SaveResultMessage(
                title = stringResource(if (isResultUnknown) R.string.save_unknown_title else R.string.save_failure_title),
                description = stringResource(if (isResultUnknown) R.string.save_unknown_description else R.string.save_failure_description),
                isSuccess = false,
            )
            Spacer(modifier = Modifier.height(HeartGuardSpacing.ResultCardGap))

            SaveErrorDetailCard(
                title = stringResource(R.string.save_error_title),
                details = errorDetails,
            )
            Spacer(modifier = Modifier.height(HeartGuardSpacing.ResultCardGap))
        }
        BottomActionBar {
            RetryButton(
                title = stringResource(if (isResultUnknown) R.string.save_check_history else R.string.save_retry),
                onClick = onRetryClick,
            )
            SaveDraftExitButton(
                title = stringResource(if (isResultUnknown) R.string.save_unknown_exit else R.string.save_draft_exit),
                onClick = onSaveDraftAndExitClick,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 683)
@Composable
private fun SaveFailureScreenPreview() {
    HeartGuardTheme {
        SaveFailureScreen(
            errorDetails = listOf(
                stringResource(R.string.save_error_network),
                stringResource(R.string.save_error_retry),
            ),
            onRetryClick = {},
            onSaveDraftAndExitClick = {},
        )
    }
}
