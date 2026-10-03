package com.nativelap.heartguard.data.record.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.map
import com.nativelap.heartguard.data.record.local.RecordSubmissionLocalDataSource
import com.nativelap.heartguard.data.record.local.StoredRecordSubmission
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.domain.record.model.PendingRecordSubmission
import com.nativelap.heartguard.domain.record.repository.RecordSubmissionRepository
import java.time.OffsetDateTime
import javax.inject.Inject

class RecordSubmissionRepositoryImpl @Inject constructor(
    private val localDataSource: RecordSubmissionLocalDataSource,
) : RecordSubmissionRepository {
    override suspend fun getPendingSubmissions(userId: String): ApiResult<List<PendingRecordSubmission>> {
        return localDataSource.read(userId).map { entries ->
            entries.map { entry ->
                PendingRecordSubmission(
                    submissionId = entry.submissionId,
                    type = FieldRecordType.valueOf(entry.type),
                    measuredAt = OffsetDateTime.parse(entry.measuredAt),
                )
            }
        }
    }

    override suspend fun beginSubmission(userId: String, submission: PendingRecordSubmission): ApiResult<Boolean> {
        return localDataSource.begin(
            userId = userId,
            submission = StoredRecordSubmission(
                submissionId = submission.submissionId,
                type = submission.type.name,
                measuredAt = submission.measuredAt.toString(),
            ),
        )
    }

    override suspend fun completeSubmission(userId: String, submissionId: String): ApiResult<Unit> {
        return localDataSource.complete(userId, submissionId)
    }
}
