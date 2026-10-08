package com.nativelap.heartguard.domain.record.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.record.model.PendingRecordSubmission

interface RecordSubmissionRepository {
    suspend fun getPendingSubmissions(userId: String): ApiResult<List<PendingRecordSubmission>>

    suspend fun beginSubmission(
        userId: String,
        submission: PendingRecordSubmission,
    ): ApiResult<Boolean>

    suspend fun completeSubmission(
        userId: String,
        submissionId: String,
    ): ApiResult<Unit>
}
