package com.nativelap.heartguard.domain.record.usecase

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.domain.profile.usecase.GetWorkerProfileUseCase
import com.nativelap.heartguard.domain.record.model.PendingRecordSubmission
import com.nativelap.heartguard.domain.record.repository.RecordSubmissionRepository
import javax.inject.Inject

class GetPendingRecordSubmissionsUseCase @Inject constructor(
    private val submissionRepository: RecordSubmissionRepository,
    private val getWorkerProfileUseCase: GetWorkerProfileUseCase,
    private val sessionManager: SessionManager,
) {
    /** 현재 계정의 미확정 제출만 읽고 조회 중 세션이 바뀌면 결과를 폐기한다. */
    suspend operator fun invoke(): ApiResult<List<PendingRecordSubmission>> {
        val owningGeneration = sessionManager.getSnapshot().generation
        val profile = getWorkerProfileUseCase()
        if (owningGeneration != sessionManager.getSnapshot().generation) {
            return ApiResult.Failure(ApiError.SessionChanged)
        }
        val userId = when (profile) {
            is ApiResult.Success -> profile.value.userId.takeIf { it.isNotBlank() }
                ?: return ApiResult.Failure(ApiError.Serialization)
            is ApiResult.Failure -> return profile
        }
        val submissions = submissionRepository.getPendingSubmissions(userId)
        return if (owningGeneration == sessionManager.getSnapshot().generation) {
            submissions
        } else {
            ApiResult.Failure(ApiError.SessionChanged)
        }
    }
}
