package com.nativelap.heartguard.data.emergency.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.emergency.mapper.toDomain
import com.nativelap.heartguard.data.emergency.remote.EmergencyCallRemoteDataSource
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallStatus
import com.nativelap.heartguard.domain.emergency.repository.EmergencyCallRepository
import javax.inject.Inject

class EmergencyCallRepositoryImpl @Inject constructor(
    private val emergencyCallRemoteDataSource: EmergencyCallRemoteDataSource,
) : EmergencyCallRepository {

    override suspend fun registerEmergencyCall(message: String?): ApiResult<EmergencyCallStatus> =
        when (val result = emergencyCallRemoteDataSource.registerEmergencyCall(message)) {
            is ApiResult.Success -> ApiResult.Success(result.value.toDomain())
            is ApiResult.Failure -> result
        }

    override suspend fun getCurrentEmergencyCallStatus(): ApiResult<EmergencyCallStatus> =
        when (val result = emergencyCallRemoteDataSource.getCurrentEmergencyCall()) {
            is ApiResult.Success -> ApiResult.Success(result.value.toDomain())
            is ApiResult.Failure -> result
        }
}
