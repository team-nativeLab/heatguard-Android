package com.nativelap.heartguard.data.emergency.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.map
import com.nativelap.heartguard.data.emergency.mapper.toDomain
import com.nativelap.heartguard.data.emergency.remote.EmergencyCallRemoteDataSource
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallStatus
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallUpdateStatus
import com.nativelap.heartguard.domain.emergency.repository.EmergencyCallRepository
import java.time.Instant
import javax.inject.Inject

class EmergencyCallRepositoryImpl @Inject constructor(
    private val emergencyCallRemoteDataSource: EmergencyCallRemoteDataSource,
) : EmergencyCallRepository {

    override suspend fun registerEmergencyCall(
        idempotencyKey: String,
        clientOccurredAt: Instant,
        message: String?,
    ): ApiResult<EmergencyCallStatus> = emergencyCallRemoteDataSource
        .registerEmergencyCall(
            idempotencyKey = idempotencyKey,
            clientOccurredAt = clientOccurredAt,
            message = message,
        )
        .map { callResponse -> callResponse.toDomain() }

    override suspend fun getCurrentEmergencyCallStatus(): ApiResult<EmergencyCallStatus> =
        emergencyCallRemoteDataSource.getCurrentEmergencyCall().map { it.toDomain() }

    override suspend fun updateEmergencyCallStatus(
        callId: String,
        status: EmergencyCallUpdateStatus,
    ): ApiResult<EmergencyCallStatus> = emergencyCallRemoteDataSource
        .updateEmergencyCallStatus(callId, status)
        .map { it.toDomain() }
}
