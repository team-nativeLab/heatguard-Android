package com.nativelap.heartguard.domain.emergency.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallStatus
import com.nativelap.heartguard.domain.emergency.repository.EmergencyCallRepository
import javax.inject.Inject

/** 팀의 가장 최근 긴급호출을 한 번 조회한다. 이미 진행 중인 호출을 이어받을 때 쓴다(종료된 호출이 올 수도 있다). */
class GetCurrentEmergencyCallUseCase @Inject constructor(
    private val emergencyCallRepository: EmergencyCallRepository,
) {
    suspend operator fun invoke(): ApiResult<EmergencyCallStatus> = emergencyCallRepository.getCurrentEmergencyCallStatus()
}
