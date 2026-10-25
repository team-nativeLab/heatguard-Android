package com.nativelap.heartguard.domain.record.model

import com.nativelap.heartguard.core.network.ApiError

sealed interface RecordSubmitResult {
    data class Saved(
        val record: FieldRecord,
        val pendingCleanup: RecordSubmissionCleanup? = null,
    ) : RecordSubmitResult

    data class NotSaved(
        val error: ApiError,
    ) : RecordSubmitResult

    data class Unknown(
        val error: ApiError,
    ) : RecordSubmitResult
}
