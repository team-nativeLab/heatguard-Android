package com.nativelap.heartguard.domain.profile.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.profile.model.WorkerProfile
import com.nativelap.heartguard.domain.profile.repository.WorkerProfileRepository
import javax.inject.Inject

/** 메뉴 드로어와 내 정보 수정 화면이 표시할 작업자 계정 정보를 조회한다. */
class GetWorkerProfileUseCase @Inject constructor(
    private val workerProfileRepository: WorkerProfileRepository,
) {
    suspend operator fun invoke(): ApiResult<WorkerProfile> = workerProfileRepository.getWorkerProfile()
}
