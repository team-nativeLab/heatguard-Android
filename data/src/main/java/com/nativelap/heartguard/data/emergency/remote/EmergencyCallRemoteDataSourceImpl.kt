package com.nativelap.heartguard.data.emergency.remote

import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.requireSuccessData
import com.nativelap.heartguard.data.emergency.dto.EmergencyCallRequestDto
import com.nativelap.heartguard.data.emergency.dto.EmergencyCallResponseDto
import com.nativelap.heartguard.data.emergency.dto.UpdateEmergencyCallStatusRequestDto
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallUpdateStatus
import java.time.Instant
import javax.inject.Inject

class EmergencyCallRemoteDataSourceImpl
    @Inject
    constructor(
        private val emergencyCallApiService: EmergencyCallApiService,
        private val apiExecutor: ApiExecutor,
    ) : EmergencyCallRemoteDataSource {
        /** 긴급호출을 등록한다. [idempotencyKey]는 사용자의 호출 의도 1회마다 하나이며, 재시도할 때 같은 키를 보내
         * 응답을 잃어버린 뒤 다시 눌러도 서버에 호출이 두 번 생기지 않게 한다(API 명세서 '기타' 항목). */
        override suspend fun registerEmergencyCall(
            idempotencyKey: String,
            clientOccurredAt: Instant,
            message: String?,
        ): ApiResult<EmergencyCallResponseDto> =
            apiExecutor
                .execute {
                    emergencyCallApiService.registerEmergencyCall(
                        idempotencyKey = idempotencyKey,
                        request =
                            EmergencyCallRequestDto(
                                message = message,
                                clientOccurredAt = clientOccurredAt.toString(),
                            ),
                    )
                }.requireSuccessData()

        override suspend fun getCurrentEmergencyCall(): ApiResult<EmergencyCallResponseDto> =
            apiExecutor
                .execute {
                    emergencyCallApiService.getCurrentEmergencyCall()
                }.requireSuccessData()

        override suspend fun updateEmergencyCallStatus(
            callId: String,
            status: EmergencyCallUpdateStatus,
        ): ApiResult<EmergencyCallResponseDto> =
            apiExecutor
                .execute {
                    emergencyCallApiService.updateEmergencyCallStatus(
                        callId = callId,
                        request =
                            UpdateEmergencyCallStatusRequestDto(
                                status = status.name,
                            ),
                    )
                }.requireSuccessData()
    }
