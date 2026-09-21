package com.nativelap.heartguard.view.route.feedback

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.view.component.feedback.SavedRecordSummaryItem
import com.nativelap.heartguard.view.screen.feedback.SaveFailureScreen
import com.nativelap.heartguard.view.screen.feedback.SaveSuccessScreen

/** 저장 성공 결과의 샘플 데이터와 완료 Navigation callback을 연결한다. */
@Composable
internal fun HeartGuardSaveSuccessRoute(
    onCompleteClick: () -> Unit,
) {
    SaveSuccessScreen(
        records = listOf(
            SavedRecordSummaryItem(
                label = stringResource(R.string.save_temperature_summary),
                value = "47.5°C",
                hasDetails = true,
            ),
            SavedRecordSummaryItem(
                label = stringResource(R.string.save_work_photo_summary),
                value = "2장",
                hasDetails = true,
            ),
            SavedRecordSummaryItem(
                label = stringResource(R.string.save_rest_photo_summary),
                value = "2장",
                hasDetails = true,
            ),
            SavedRecordSummaryItem(
                label = stringResource(R.string.save_time_summary),
                value = stringResource(R.string.save_time_value),
            ),
        ),
        onCompleteClick = onCompleteClick,
    )
}

/** 저장 실패 결과와 재시도 Navigation callback을 연결한다. */
@Composable
internal fun HeartGuardSaveFailureRoute(
    onRetryClick: () -> Unit,
    onSaveDraftAndExitClick: () -> Unit,
) {
    SaveFailureScreen(
        errorDetails = listOf(
            stringResource(R.string.save_error_network),
            stringResource(R.string.save_error_retry),
        ),
        onRetryClick = onRetryClick,
        onSaveDraftAndExitClick = onSaveDraftAndExitClick,
    )
}
