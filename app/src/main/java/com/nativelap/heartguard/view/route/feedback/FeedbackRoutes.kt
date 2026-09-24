package com.nativelap.heartguard.view.route.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.view.component.feedback.SavedRecordSummaryItem
import com.nativelap.heartguard.view.screen.feedback.SaveConfirmationScreen
import com.nativelap.heartguard.view.screen.feedback.SaveFailureScreen
import com.nativelap.heartguard.view.screen.feedback.SaveSuccessScreen
import com.nativelap.heartguard.viewmodel.record.RecordDraftViewModel

/** 저장 전 입력 내용 확인 화면에 [recordDraftViewModel]이 들고 있는 실제 온도·사진 값을 전달한다. */
@Composable
internal fun HeartGuardSaveConfirmationRoute(
    recordDraftViewModel: RecordDraftViewModel,
    onCaptureClick: () -> Unit,
    onSaveClick: () -> Unit,
) {
    val draftState by recordDraftViewModel.uiState.collectAsState()

    SaveConfirmationScreen(
        isTemperatureSaved = draftState.isTemperatureSaved,
        temperatureText = draftState.temperatureText,
        humidityText = draftState.humidityText,
        // 체감온도는 TemperatureRecord/FieldPhoto 화면과 마찬가지로 사용자가 직접 입력하는 값이
        // 아니라 서버가 계산해 내려주는 값이므로, 실제 API 연동 전까지는 다른 화면과 동일한
        // 고정 표시값을 그대로 사용한다.
        feelsLikeText = "40.5",
        fieldPhotoUris = draftState.fieldPhotoUris,
        onRemoveFieldPhoto = recordDraftViewModel::removeFieldPhoto,
        onCaptureClick = onCaptureClick,
        onSaveClick = onSaveClick,
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
                label = stringResource(R.string.save_temperature_summary),
                value = "47.5 ℃",
                hasDetails = true,
                detail = "( 습도 55% 체감 40.5℃ )",
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
