package com.nativelap.heartguard.view.route.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.view.component.feedback.SavedRecordSummaryItem
import com.nativelap.heartguard.view.screen.feedback.SaveConfirmationScreen
import com.nativelap.heartguard.view.screen.feedback.SaveFailureScreen
import com.nativelap.heartguard.view.screen.feedback.SaveSuccessScreen
import com.nativelap.heartguard.viewmodel.record.RecordDraftViewModel
import com.nativelap.heartguard.viewmodel.record.RecordSubmissionState
import kotlinx.coroutines.launch

/** 저장 전 입력 내용 확인 화면에 [recordDraftViewModel]이 들고 있는 실제 온도·사진 값을 전달하고,
 * "저장" 클릭 시 실제 업로드·기록등록 API를 호출해 결과에 따라 성공/실패 화면으로 이동한다.
 * 상태 변화를 구독하는 대신 [RecordDraftViewModel.submit] 호출 결과를 직접 받아 내비게이션하므로,
 * 실패 후 "다시 시도하기"로 이 화면에 되돌아왔을 때 이전 실패 결과로 다시 자동 이동하지 않는다. */
@Composable
internal fun HeartGuardSaveConfirmationRoute(
    recordDraftViewModel: RecordDraftViewModel,
    onCaptureClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    onSaveFailure: () -> Unit,
) {
    val draftState by recordDraftViewModel.uiState.collectAsState()
    val submissionState by recordDraftViewModel.submissionState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    SaveConfirmationScreen(
        isTemperatureSaved = draftState.isTemperatureSaved,
        temperatureText = draftState.temperatureText,
        humidityText = draftState.humidityText,
        // 체감온도는 TemperatureRecord/FieldPhoto 화면과 마찬가지로 사용자가 직접 입력하는 값이
        // 아니라 서버가 계산해 내려주는 값이므로, 저장 전 화면에서는 다른 화면과 동일한 고정
        // 표시값을 그대로 쓰고 실제 값은 저장 응답(FieldRecord.apparentTemperature)에서 받는다.
        feelsLikeText = "40.5",
        fieldPhotoUris = draftState.fieldPhotoUris,
        onRemoveFieldPhoto = recordDraftViewModel::removeFieldPhoto,
        onCaptureClick = onCaptureClick,
        onSaveClick = {
            // 이미 제출이 진행 중이면 연타로 두 번째 submit()이 겹쳐 시작되지 않도록 막는다.
            if (submissionState !is RecordSubmissionState.Submitting) {
                coroutineScope.launch {
                    val submitResult = recordDraftViewModel.submit()
                    if (submitResult is ApiResult.Success) {
                        onSaveSuccess()
                    } else {
                        onSaveFailure()
                    }
                }
            }
        },
    )
}

/** 저장 성공 결과를 [recordDraftViewModel]의 실제 입력값과 저장 응답으로 보여준다. */
@Composable
internal fun HeartGuardSaveSuccessRoute(
    recordDraftViewModel: RecordDraftViewModel,
    onCompleteClick: () -> Unit,
) {
    val draftState by recordDraftViewModel.uiState.collectAsState()
    val submissionState by recordDraftViewModel.submissionState.collectAsState()
    val record = (submissionState as? RecordSubmissionState.Success)?.record

    SaveSuccessScreen(
        records = listOf(
            SavedRecordSummaryItem(
                label = stringResource(R.string.save_temperature_summary),
                value = "${draftState.temperatureText} ℃",
                hasDetails = true,
                detail = "( 습도 ${draftState.humidityText}% 체감 ${record?.apparentTemperature ?: "-"}℃ )",
            ),
            SavedRecordSummaryItem(
                label = stringResource(R.string.save_work_photo_summary),
                value = stringResource(R.string.photo_selected_count, draftState.workPhotoUris.size),
                hasDetails = true,
            ),
            SavedRecordSummaryItem(
                label = stringResource(R.string.save_rest_photo_summary),
                value = stringResource(R.string.photo_selected_count, draftState.restPhotoUris.size),
                hasDetails = true,
            ),
            SavedRecordSummaryItem(
                label = stringResource(R.string.save_time_summary),
                value = stringResource(R.string.save_time_value),
            ),
        ),
        // 기록 완료 후 홈으로 돌아가기 전에 draft(임시 사진 파일 포함)를 정리한다.
        onCompleteClick = {
            recordDraftViewModel.reset()
            onCompleteClick()
        },
    )
}

/** 저장 실패 결과를 [recordDraftViewModel]의 실제 응답 오류로 보여주고 재시도 Navigation callback을 연결한다. */
@Composable
internal fun HeartGuardSaveFailureRoute(
    recordDraftViewModel: RecordDraftViewModel,
    onRetryClick: () -> Unit,
    onSaveDraftAndExitClick: () -> Unit,
) {
    val submissionState by recordDraftViewModel.submissionState.collectAsState()
    val error = (submissionState as? RecordSubmissionState.Failure)?.error

    SaveFailureScreen(
        errorDetails = listOf(
            error?.toDisplayMessage() ?: stringResource(R.string.save_error_network),
            stringResource(R.string.save_error_retry),
        ),
        onRetryClick = onRetryClick,
        onSaveDraftAndExitClick = onSaveDraftAndExitClick,
    )
}

@Composable
private fun ApiError.toDisplayMessage(): String = when (this) {
    is ApiError.Http -> stringResource(R.string.save_error_http, statusCode)
    ApiError.Network -> stringResource(R.string.save_error_network)
    ApiError.Serialization -> stringResource(R.string.save_error_serialization)
    ApiError.Unknown -> stringResource(R.string.save_error_unknown)
}
