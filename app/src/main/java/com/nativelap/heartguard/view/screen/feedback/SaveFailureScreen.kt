package com.nativelap.heartguard.view.screen.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.feedback.RetryButton
import com.nativelap.heartguard.view.component.feedback.SaveDraftExitButton
import com.nativelap.heartguard.view.component.feedback.SaveErrorDetailCard
import com.nativelap.heartguard.view.component.feedback.SaveResultMessage
import com.nativelap.heartguard.view.component.feedback.SaveResultTitle

/** 저장 실패 메시지와 원인·재시도 동작을 결과 화면으로 조합한다. */
@Composable
fun SaveFailureScreen(
    errorDetails: List<String>,
    onRetryClick: () -> Unit,
    onSaveDraftAndExitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(HeartGuardSpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Section),
        ) {
            SaveResultTitle(
                title = stringResource(R.string.save_record_title),
            )

            SaveResultMessage(
                title = stringResource(R.string.save_failure_title),
                description = stringResource(R.string.save_failure_description),
                isSuccess = false,
            )

            SaveErrorDetailCard(
                title = stringResource(R.string.save_error_title),
                details = errorDetails,
            )

            RetryButton(
                title = stringResource(R.string.common_retry),
                onClick = onRetryClick,
            )

            SaveDraftExitButton(
                title = stringResource(R.string.save_draft_exit),
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
