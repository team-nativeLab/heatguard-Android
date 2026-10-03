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
import com.nativelap.heartguard.view.component.feedback.SaveErrorDetailCard
import com.nativelap.heartguard.view.component.feedback.SaveCompleteButton
import com.nativelap.heartguard.view.component.feedback.SaveResultMessage
import com.nativelap.heartguard.view.component.feedback.SaveResultTitle
import com.nativelap.heartguard.view.component.feedback.SavedRecordSummaryCard
import com.nativelap.heartguard.view.component.feedback.SavedRecordSummaryItem

/** 기록 저장 성공 메시지와 저장된 항목 요약을 바텀시트(Figma 09)로 구성한다.
 * [onDetailsClick]이 있으면 요약 행을 눌러 방금 저장한 기록 상세로 이동한다. */
@Composable
fun SaveSuccessScreen(
    records: List<SavedRecordSummaryItem>,
    onCompleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDetailsClick: (() -> Unit)? = null,
    hasPendingCleanup: Boolean = false,
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
                title = stringResource(R.string.save_success_title),
                description = stringResource(R.string.save_success_description),
                isSuccess = true,
            )
            Spacer(modifier = Modifier.height(HeartGuardSpacing.ResultCardGap))

            if (hasPendingCleanup) {
                SaveErrorDetailCard(
                    title = stringResource(R.string.save_cleanup_title),
                    details = listOf(stringResource(R.string.save_cleanup_description)),
                )

                Spacer(modifier = Modifier.height(HeartGuardSpacing.ResultCardGap))
            }

            SavedRecordSummaryCard(
                title = stringResource(R.string.save_record_summary),
                records = records,
                onDetailsClick = onDetailsClick,
            )
            Spacer(modifier = Modifier.height(HeartGuardSpacing.ResultSuccessButtonGap))
        }
        BottomActionBar {
            SaveCompleteButton(
                title = stringResource(R.string.save_complete),
                onClick = onCompleteClick,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 683)
@Composable
private fun SaveSuccessScreenPreview() {
    HeartGuardTheme {
        SaveSuccessScreen(
            records = listOf(
                SavedRecordSummaryItem(
                    label = "현재 온도",
                    value = "37℃",
                ),
                SavedRecordSummaryItem(
                    label = "체감온도",
                    value = "40℃",
                ),
                SavedRecordSummaryItem(
                    label = "습도",
                    value = "65%",
                ),
            ),
            onCompleteClick = {},
        )
    }
}
