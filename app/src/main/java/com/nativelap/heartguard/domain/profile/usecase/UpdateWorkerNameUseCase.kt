package com.nativelap.heartguard.domain.profile.usecase

import com.nativelap.heartguard.domain.profile.model.ProfileUpdateResult
import com.nativelap.heartguard.domain.profile.repository.WorkerProfileRepository
import javax.inject.Inject

/** 내 정보 수정 화면에서 입력한 이름을 앞뒤 공백을 제거해 저장한다.
 * 빈 이름은 서버 검증 오류가 되므로 호출 전에 화면이 저장 버튼을 막는다. [version]은 마지막으로 조회한 버전이다. */
class UpdateWorkerNameUseCase @Inject constructor(
    private val workerProfileRepository: WorkerProfileRepository,
) {
    suspend operator fun invoke(
        name: String,
        version: Long?,
    ): ProfileUpdateResult = workerProfileRepository.updateWorkerName(
        name = name.trim(),
        version = version,
    )
}
