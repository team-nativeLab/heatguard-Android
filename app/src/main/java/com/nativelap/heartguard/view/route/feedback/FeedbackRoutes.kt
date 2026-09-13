package com.nativelap.heartguard.view.route.feedback

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.view.component.feedback.SaveConfirmationDialog
import com.nativelap.heartguard.view.component.feedback.SavedRecordSummaryItem
import com.nativelap.heartguard.view.screen.feedback.SaveFailureScreen
import com.nativelap.heartguard.view.screen.feedback.SaveSuccessScreen

/** 저장 확인 Dialog의 UI와 Navigation callback을 연결하는 Route adapter이다. */
@Composable
internal fun HeartGuardSaveConfirmationRoute(
    onDismissClick: () -> Unit,
    onConfirmClick: () -> Unit,
) {
    SaveConfirmationDialog(
        onDismissClick = onDismissClick,
        onConfirmClick = onConfirmClick,
        modifier = Modifier.widthIn(max = 320.dp),
    )
}

/** 저장 성공 결과의 샘플 데이터와 완료 Navigation callback을 연결한다. */
@Composable
internal fun HeartGuardSaveSuccessRoute(
    onCompleteClick: () -> Unit,
) {
    SaveSuccessScreen(
        records = listOf(
            SavedRecordSummaryItem(
                label = stringResource(R.string.home_current_temperature),
                value = "37℃",
            ),
            SavedRecordSummaryItem(
                label = stringResource(R.string.home_feels_like),
                value = "40℃",
            ),
            SavedRecordSummaryItem(
                label = stringResource(R.string.home_humidity),
                value = "65%",
            ),
        ),
        onCompleteClick = onCompleteClick,
    )
}

/** 저장 실패 결과와 재시도 Navigation callback을 연결한다. */
@Composable
internal fun HeartGuardSaveFailureRoute(
    onRetryClick: () -> Unit,
) {
    SaveFailureScreen(
        errorDetails = listOf(
            stringResource(R.string.save_error_network),
            stringResource(R.string.save_failure_description),
        ),
        onRetryClick = onRetryClick,
    )
}
