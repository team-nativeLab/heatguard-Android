package com.nativelap.heartguard.domain.emergency.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallStatus
import com.nativelap.heartguard.domain.emergency.repository.EmergencyCallRepository
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

/** 작업자 화면의 3초 폴링(API 명세서 '긴급호출 상태 조회' 기타 항목)을 Flow로 감싸,
 * ViewModel이 직접 반복문을 돌리지 않고 collect만 하면 되게 한다. */
class ObserveEmergencyCallStatusUseCase @Inject constructor(
    private val emergencyCallRepository: EmergencyCallRepository,
) {
    operator fun invoke(): Flow<ApiResult<EmergencyCallStatus>> = flow {
        while (coroutineContext.isActive) {
            emit(emergencyCallRepository.getCurrentEmergencyCallStatus())
            delay(POLLING_INTERVAL_MS)
        }
    }

    private companion object {
        const val POLLING_INTERVAL_MS = 3_000L
    }
}
