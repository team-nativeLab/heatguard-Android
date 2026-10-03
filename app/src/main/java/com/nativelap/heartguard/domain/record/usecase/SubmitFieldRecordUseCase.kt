package com.nativelap.heartguard.domain.record.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.domain.profile.usecase.GetWorkerProfileUseCase
import com.nativelap.heartguard.domain.record.model.PendingRecordSubmission
import com.nativelap.heartguard.domain.record.model.RecordSubmitResult
import com.nativelap.heartguard.domain.record.model.RecordSubmissionCleanup
import com.nativelap.heartguard.domain.record.repository.RecordSubmissionRepository
import com.nativelap.heartguard.domain.record.model.FieldRecord
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.domain.record.repository.RecordRepository
import java.time.OffsetDateTime
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class SubmitFieldRecordUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val submissionRepository: RecordSubmissionRepository,
    private val getWorkerProfileUseCase: GetWorkerProfileUseCase,
    private val sessionManager: SessionManager,
) {
    /** 제출 표식을 먼저 영속화하고 서버 저장 여부가 불명인 제출은 재전송하지 않는다. */
    suspend operator fun invoke(
        submissionId: String,
        expectedSessionGeneration: Long = sessionManager.getSnapshot().generation,
        type: FieldRecordType,
        photoKeys: List<String>,
        measuredAt: OffsetDateTime,
        temperature: Double?,
        humidity: Double?,
        memo: String?,
        restStartedAt: OffsetDateTime?,
        restEndedAt: OffsetDateTime?,
    ): RecordSubmitResult {
        val owningGeneration = expectedSessionGeneration
        if (owningGeneration != sessionManager.getSnapshot().generation) {
            return RecordSubmitResult.NotSaved(ApiError.SessionChanged)
        }
        val profileResult = getWorkerProfileUseCase()
        if (owningGeneration != sessionManager.getSnapshot().generation) {
            return RecordSubmitResult.NotSaved(ApiError.SessionChanged)
        }
        val userId = when (profileResult) {
            is ApiResult.Success -> profileResult.value.userId.takeIf { it.isNotBlank() }
                ?: return RecordSubmitResult.NotSaved(ApiError.Serialization)
            is ApiResult.Failure -> return RecordSubmitResult.NotSaved(profileResult.error)
        }
        val beginResult = submissionRepository.beginSubmission(
            userId,
            PendingRecordSubmission(submissionId, type, measuredAt),
        )
        when (beginResult) {
            is ApiResult.Failure -> return RecordSubmitResult.NotSaved(beginResult.error)
            is ApiResult.Success -> if (!beginResult.value) {
                return RecordSubmitResult.Unknown(ApiError.Unknown)
            }
        }
        val submitResult = if (owningGeneration != sessionManager.getSnapshot().generation) {
            ApiResult.Failure(ApiError.SessionChanged)
        } else {
            recordRepository.submitFieldRecord(
                expectedSessionGeneration = owningGeneration,
                type = type,
                photoKeys = photoKeys,
                measuredAt = measuredAt,
                temperature = temperature,
                humidity = humidity,
                memo = memo,
                restStartedAt = restStartedAt,
                restEndedAt = restEndedAt,
            )
        }
        val outcome = when (submitResult) {
            is ApiResult.Success -> if (submitResult.value.recordId.isNotBlank()) {
                RecordSubmitResult.Saved(submitResult.value)
            } else {
                RecordSubmitResult.Unknown(ApiError.Serialization)
            }
            is ApiResult.Failure -> if (submitResult.error.isDefinitiveRejection()) {
                RecordSubmitResult.NotSaved(submitResult.error)
            } else {
                RecordSubmitResult.Unknown(submitResult.error)
            }
        }
        if (outcome !is RecordSubmitResult.Unknown) {
            val completion = withContext(NonCancellable) {
                submissionRepository.completeSubmission(userId, submissionId)
            }
            if (completion is ApiResult.Failure) {
                return when (outcome) {
                    is RecordSubmitResult.Saved -> outcome.copy(
                        pendingCleanup = RecordSubmissionCleanup(userId, submissionId),
                    )
                    else -> RecordSubmitResult.Unknown(completion.error)
                }
            }
        }
        return outcome
    }

    private fun ApiError.isDefinitiveRejection(): Boolean {
        if (this == ApiError.SessionChanged) {
            return true
        }
        val rejectionCode = when (this) {
            is ApiError.Http -> if (statusCode in 400..499) errorCode else null
            is ApiError.ServerRejected -> errorCode
            else -> null
        }
        return rejectionCode in DEFINITIVE_REJECTIONS
    }

    private companion object {
        val DEFINITIVE_REJECTIONS = setOf(
            "VALIDATION_ERROR",
            "UNAUTHORIZED",
            "FORBIDDEN",
            "UPLOAD_NOT_FOUND",
            "WEATHER_BASELINE_REQUIRED",
            "RATE_LIMITED",
        )
    }
}
