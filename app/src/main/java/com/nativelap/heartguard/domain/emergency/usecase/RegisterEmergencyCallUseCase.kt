package com.nativelap.heartguard.domain.emergency.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallStatus
import com.nativelap.heartguard.domain.emergency.repository.EmergencyCallRepository
import java.time.Instant
import javax.inject.Inject

/** 긴급호출을 등록한다. [idempotencyKey]는 호출 의도 1회에 하나를 만들어 재시도에도 그대로 쓴다.
 * 이미 진행 중인 호출이 있으면 서버가 409 ACTIVE_CALL_ALREADY_EXISTS로 응답하며, 호출부는 현재 호출을 조회해 이어 간다. */
class RegisterEmergencyCallUseCase @Inject constructor(
    private val emergencyCallRepository: EmergencyCallRepository,
) {
    suspend operator fun invoke(
        idempotencyKey: String,
        clientOccurredAt: Instant,
        message: String? = null,
    ): ApiResult<EmergencyCallStatus> = emergencyCallRepository.registerEmergencyCall(
        idempotencyKey = idempotencyKey,
        clientOccurredAt = clientOccurredAt,
        message = message,
    )
}
