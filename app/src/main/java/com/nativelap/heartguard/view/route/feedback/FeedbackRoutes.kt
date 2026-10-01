package com.nativelap.heartguard.view.route.feedback

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nativelap.heartguard.R
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.view.component.RecordType
import com.nativelap.heartguard.view.component.feedback.SavedRecordSummaryItem
import com.nativelap.heartguard.view.component.humidityValueText
import com.nativelap.heartguard.view.component.temperatureValueText
import com.nativelap.heartguard.view.component.valueOrEmptyText
import com.nativelap.heartguard.view.screen.feedback.SaveFailureScreen
import com.nativelap.heartguard.view.screen.feedback.SaveSuccessScreen
import com.nativelap.heartguard.viewmodel.record.RecordDraftUiState
import com.nativelap.heartguard.viewmodel.record.RecordDraftViewModel
import com.nativelap.heartguard.viewmodel.record.RecordSubmissionState
import com.nativelap.heartguard.viewmodel.toDisplayNumber
import java.time.format.DateTimeFormatter

/** 저장 성공 화면이다. 방금 저장한 기록 종류의 요약과 서버가 기록한 저장 시각만 보여준다.
 * 서버에 보내지 않은 값은 표시하지 않으며, 응답에 없는 값(체감온도·저장 시각 등)은 "--"로 둔다. */
@Composable
internal fun HeartGuardSaveSuccessRoute(
    recordDraftViewModel: RecordDraftViewModel,
    onCompleteClick: () -> Unit,
    onRecordDetailClick: (String) -> Unit,
) {
    val draftState by recordDraftViewModel.uiState.collectAsStateWithLifecycle()
    val submissionState by recordDraftViewModel.submissionState.collectAsStateWithLifecycle()
    val record = (submissionState as? RecordSubmissionState.Success)?.record
    val savedRecordId = record?.recordId
    val completeRecord = {
        recordDraftViewModel.reset()
        onCompleteClick()
    }

    // 시스템 뒤로가기도 "확인"과 같이 기록을 마치고 홈으로 간다(입력 화면으로 돌아가 중복 저장하지 않게).
    BackHandler(onBack = completeRecord)
    val recordSummaryItems = when (draftState.selectedRecordType) {
        RecordType.TEMPERATURE -> temperatureRecordSummaryItems(
            draftState = draftState,
            apparentTemperature = record?.apparentTemperature,
        )

        RecordType.WORK -> listOf(
            SavedRecordSummaryItem(
                label = stringResource(R.string.save_work_photo_summary),
                value = stringResource(R.string.save_photo_count_format, draftState.workPhotoUris.size),
                hasDetails = true,
            ),
        )

        RecordType.REST -> listOf(
            SavedRecordSummaryItem(
                label = stringResource(R.string.save_rest_photo_summary),
                value = stringResource(R.string.save_photo_count_format, draftState.restPhotoUris.size),
                hasDetails = true,
            ),
        )

        null -> emptyList()
    }

    SaveSuccessScreen(
        records = recordSummaryItems + SavedRecordSummaryItem(
            label = stringResource(R.string.save_time_summary),
            value = valueOrEmptyText(record?.createdAt?.format(savedAtFormatter)),
        ),
        // 기록 완료 후 홈으로 돌아가기 전에 draft(임시 사진 파일 포함)를 정리한다.
        onCompleteClick = completeRecord,
        onDetailsClick = savedRecordId?.let { recordId ->
            {
                recordDraftViewModel.reset()
                onRecordDetailClick(recordId)
            }
        },
    )
}

// 온도계 기록 요약이다. 직접 입력했으면 입력한 온도·습도를, 아니면 "--"를, 체감온도는 서버 응답 값을 보여준다.
@Composable
private fun temperatureRecordSummaryItems(
    draftState: RecordDraftUiState,
    apparentTemperature: Double?,
): List<SavedRecordSummaryItem> {
    val manualTemperature = draftState.temperatureText.takeIf { draftState.isManualInputEnabled && it.isNotBlank() }
    val manualHumidity = draftState.humidityText.takeIf { draftState.isManualInputEnabled && it.isNotBlank() }
    val temperatureSummary = SavedRecordSummaryItem(
        label = stringResource(R.string.save_temperature_summary),
        value = temperatureValueText(manualTemperature),
        hasDetails = true,
        detail = stringResource(
            R.string.save_temperature_detail_format,
            humidityValueText(manualHumidity),
            temperatureValueText(apparentTemperature?.toDisplayNumber()),
        ),
    )

    if (draftState.fieldPhotoUris.isEmpty()) {
        return listOf(temperatureSummary)
    }

    return listOf(
        temperatureSummary,
        SavedRecordSummaryItem(
            label = stringResource(R.string.home_field_photo),
            value = stringResource(R.string.save_photo_count_format, draftState.fieldPhotoUris.size),
            hasDetails = true,
        ),
    )
}

// 서버 저장 시각을 Figma "2026.07.18 10 : 30" 형식으로 보여준다(날짜 패턴이라 번역 대상이 아니다).
private val savedAtFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH : mm")

/** 저장 실패 결과를 [recordDraftViewModel]의 실제 응답 오류로 보여주고 재시도 Navigation callback을 연결한다. */
@Composable
internal fun HeartGuardSaveFailureRoute(
    recordDraftViewModel: RecordDraftViewModel,
    onRetryClick: () -> Unit,
    onSaveDraftAndExitClick: () -> Unit,
) {
    val submissionState by recordDraftViewModel.submissionState.collectAsStateWithLifecycle()
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

// 명세에 정의된 기록·업로드 오류 코드는 작업자가 할 수 있는 조치를 알려 주고, 그 밖의 HTTP 오류는 상태 코드를 보여준다.
@Composable
private fun ApiError.toDisplayMessage(): String = when (this) {
    is ApiError.Http -> when (errorCode) {
        "WEATHER_BASELINE_REQUIRED" -> stringResource(R.string.save_error_weather_baseline_required)
        "UPLOAD_NOT_FOUND",
        "UPLOAD_ALREADY_USED",
        -> stringResource(R.string.save_error_upload_expired)
        "FILE_TOO_LARGE" -> stringResource(R.string.save_error_file_too_large)
        "UNSUPPORTED_MEDIA_TYPE" -> stringResource(R.string.save_error_unsupported_media_type)
        "RATE_LIMITED" -> stringResource(R.string.save_error_rate_limited)
        else -> stringResource(R.string.save_error_http, statusCode)
    }

    ApiError.Network -> stringResource(R.string.save_error_network)
    ApiError.Serialization -> stringResource(R.string.save_error_serialization)
    ApiError.SessionChanged,
    ApiError.Unknown,
    -> stringResource(R.string.save_error_unknown)
}
