package com.nativelap.heartguard.domain.emergency.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallStatus
import com.nativelap.heartguard.domain.emergency.repository.EmergencyCallRepository
import javax.inject.Inject

/** 긴급호출을 등록한다. 이미 ACTIVE 호출이 있으면 서버가 409로 응답하는데, 그 경우도
 * "이미 호출 중"이라는 정상적인 상태이므로 호출부는 이 결과와 무관하게 상태 폴링을 시작하면 된다. */
class RegisterEmergencyCallUseCase @Inject constructor(
    private val emergencyCallRepository: EmergencyCallRepository,
) {
    suspend operator fun invoke(message: String? = null): ApiResult<EmergencyCallStatus> =
        emergencyCallRepository.registerEmergencyCall(message)
}
