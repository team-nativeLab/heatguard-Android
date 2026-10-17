package com.nativelap.heartguard.data.record.local

import com.nativelap.heartguard.core.network.ApiResult

interface RecordSubmissionLocalDataSource {
    suspend fun read(userId: String): ApiResult<List<StoredRecordSubmission>>

    suspend fun begin(
        userId: String,
        submission: StoredRecordSubmission,
    ): ApiResult<Boolean>

    suspend fun complete(
        userId: String,
        submissionId: String,
    ): ApiResult<Unit>
}
