package com.nativelap.heartguard.domain.record.usecase

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.domain.profile.usecase.GetWorkerProfileUseCase
import com.nativelap.heartguard.domain.record.model.RecordSubmissionCleanup
import com.nativelap.heartguard.domain.record.repository.RecordSubmissionRepository
import javax.inject.Inject

class RetryRecordSubmissionCleanupUseCase @Inject constructor(
    private val submissionRepository: RecordSubmissionRepository,
    private val getWorkerProfileUseCase: GetWorkerProfileUseCase,
    private val sessionManager: SessionManager,
) {
    /** 서버에 재전송하지 않고 현재 계정의 성공한 제출 표식만 정리한다. */
    suspend operator fun invoke(cleanup: RecordSubmissionCleanup): ApiResult<Unit> {
        val owningGeneration = sessionManager.getSnapshot().generation
        val profile = getWorkerProfileUseCase()
        if (owningGeneration != sessionManager.getSnapshot().generation) {
            return ApiResult.Failure(ApiError.SessionChanged)
        }
        when (profile) {
            is ApiResult.Failure -> return profile
            is ApiResult.Success -> if (profile.value.userId != cleanup.userId) {
                return ApiResult.Failure(ApiError.SessionChanged)
            }
        }
        return submissionRepository.completeSubmission(cleanup.userId, cleanup.submissionId)
    }
}
