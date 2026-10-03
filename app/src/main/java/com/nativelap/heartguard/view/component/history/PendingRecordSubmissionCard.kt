package com.nativelap.heartguard.view.component.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.domain.record.model.PendingRecordSubmission
import com.nativelap.heartguard.view.component.feedback.SaveErrorDetailCard
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PendingRecordSubmissionCard(submission: PendingRecordSubmission) {
    val recordTypeTitle = stringResource(
        when (submission.type) {
            FieldRecordType.THERMOMETER -> R.string.history_type_thermometer
            FieldRecordType.WORK -> R.string.history_type_work
            FieldRecordType.REST -> R.string.history_type_rest
        },
    )
    SaveErrorDetailCard(
        title = stringResource(R.string.save_unknown_title),
        details = listOf(
            stringResource(
                R.string.pending_submission_format,
                recordTypeTitle,
                submission.measuredAt.atZoneSameInstant(KOREA_ZONE).format(SUBMISSION_TIME_FORMAT),
            ),
            stringResource(R.string.save_unknown_detail),
        ),
    )
}

private val KOREA_ZONE = ZoneId.of("Asia/Seoul")
private val SUBMISSION_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
