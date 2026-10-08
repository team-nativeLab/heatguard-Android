package com.nativelap.heartguard.data.emergency.repository

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.map
import com.nativelap.heartguard.data.emergency.mapper.toDomain
import com.nativelap.heartguard.data.emergency.remote.EmergencyCallRemoteDataSource
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallStatus
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallUpdateResult
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallUpdateStatus
import com.nativelap.heartguard.domain.emergency.repository.EmergencyCallRepository
import java.time.Instant
import javax.inject.Inject

class EmergencyCallRepositoryImpl
    @Inject
    constructor(
        private val emergencyCallRemoteDataSource: EmergencyCallRemoteDataSource,
    ) : EmergencyCallRepository {
        override suspend fun registerEmergencyCall(
            idempotencyKey: String,
            clientOccurredAt: Instant,
            message: String?,
        ): ApiResult<EmergencyCallStatus> =
            emergencyCallRemoteDataSource
                .registerEmergencyCall(
                    idempotencyKey = idempotencyKey,
                    clientOccurredAt = clientOccurredAt,
                    message = message,
                ).map { callResponse -> callResponse.toDomain() }

        override suspend fun getCurrentEmergencyCallStatus(): ApiResult<EmergencyCallStatus> =
            emergencyCallRemoteDataSource.getCurrentEmergencyCall().map { it.toDomain() }

        /** 취소·종료 요청의 409 오류 코드를 이미 종료됨·전이 불가 결과로 바꾼다. */
        override suspend fun updateEmergencyCallStatus(
            callId: String,
            status: EmergencyCallUpdateStatus,
        ): EmergencyCallUpdateResult {
            val updateResult = emergencyCallRemoteDataSource.updateEmergencyCallStatus(callId, status)
            return when (updateResult) {
                is ApiResult.Success -> EmergencyCallUpdateResult.Updated(updateResult.value.toDomain())
                is ApiResult.Failure -> updateResult.error.toUpdateFailure()
            }
        }

        private fun ApiError.toUpdateFailure(): EmergencyCallUpdateResult {
            if (this !is ApiError.Http || statusCode != HTTP_CONFLICT) {
                return EmergencyCallUpdateResult.Failure
            }

            return when (errorCode) {
                EMERGENCY_CALL_CLOSED_CODE -> EmergencyCallUpdateResult.AlreadyClosed
                INVALID_STATUS_TRANSITION_CODE -> EmergencyCallUpdateResult.InvalidTransition
                else -> EmergencyCallUpdateResult.Failure
            }
        }

        private companion object {
            const val HTTP_CONFLICT = 409
            const val EMERGENCY_CALL_CLOSED_CODE = "EMERGENCY_CALL_CLOSED"
            const val INVALID_STATUS_TRANSITION_CODE = "INVALID_STATUS_TRANSITION"
        }
    }
