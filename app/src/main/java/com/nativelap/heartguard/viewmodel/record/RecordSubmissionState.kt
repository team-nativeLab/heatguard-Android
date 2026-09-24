package com.nativelap.heartguard.viewmodel.record

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.domain.record.model.FieldRecord

sealed interface RecordSubmissionState {
    data object Idle : RecordSubmissionState

    data object Submitting : RecordSubmissionState

    data class Success(val record: FieldRecord) : RecordSubmissionState

    data class Failure(val error: ApiError) : RecordSubmissionState
}
