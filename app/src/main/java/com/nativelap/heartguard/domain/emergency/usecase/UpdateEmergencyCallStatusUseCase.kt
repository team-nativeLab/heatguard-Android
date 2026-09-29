package com.nativelap.heartguard.domain.emergency.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallStatus
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallUpdateResult
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallUpdateStatus
import com.nativelap.heartguard.domain.emergency.repository.EmergencyCallRepository
import javax.inject.Inject

/**
 * 작업자의 긴급호출 취소 또는 종료 요청을 서버에 전달한다.
 * 허용 상태 전이는 API가 검증하며, 실패 시 호출 화면을 유지한다.
 */
class UpdateEmergencyCallStatusUseCase @Inject constructor(
    private val emergencyCallRepository: EmergencyCallRepository,
) {
    suspend operator fun invoke(
        callId: String,
        status: EmergencyCallUpdateStatus,
    ): EmergencyCallUpdateResult = emergencyCallRepository.updateEmergencyCallStatus(
        callId = callId,
        status = status,
    )
}
