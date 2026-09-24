package com.nativelap.heartguard.data.emergency.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.map
import com.nativelap.heartguard.data.emergency.mapper.toDomain
import com.nativelap.heartguard.data.emergency.remote.EmergencyCallRemoteDataSource
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallStatus
import com.nativelap.heartguard.domain.emergency.repository.EmergencyCallRepository
import javax.inject.Inject

class EmergencyCallRepositoryImpl @Inject constructor(
    private val emergencyCallRemoteDataSource: EmergencyCallRemoteDataSource,
) : EmergencyCallRepository {

    override suspend fun registerEmergencyCall(message: String?): ApiResult<EmergencyCallStatus> =
        emergencyCallRemoteDataSource.registerEmergencyCall(message).map { it.toDomain() }

    override suspend fun getCurrentEmergencyCallStatus(): ApiResult<EmergencyCallStatus> =
        emergencyCallRemoteDataSource.getCurrentEmergencyCall().map { it.toDomain() }
}
