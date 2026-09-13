package com.nativelap.heartguard.view.screen.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.feedback.SaveCompleteButton
import com.nativelap.heartguard.view.component.feedback.SaveResultMessage
import com.nativelap.heartguard.view.component.feedback.SavedRecordSummaryCard
import com.nativelap.heartguard.view.component.feedback.SavedRecordSummaryItem

/** 기록 저장 성공 메시지와 저장된 항목 요약을 하나의 결과 화면으로 구성한다. */
@Composable
fun SaveSuccessScreen(
    records: List<SavedRecordSummaryItem>,
    onCompleteClick: () -> Unit,
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
            SaveResultMessage(
                resultIconPainter = rememberVectorPainter(Icons.Outlined.CheckCircle),
                resultIconContentDescription = stringResource(R.string.save_success_title),
                title = stringResource(R.string.save_success_title),
                description = stringResource(R.string.save_success_description),
                isSuccess = true,
            )

            SavedRecordSummaryCard(
                title = stringResource(R.string.save_record_summary),
                records = records,
            )

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
