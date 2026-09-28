package com.nativelap.heartguard.data.profile.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.map
import com.nativelap.heartguard.data.profile.mapper.toDomain
import com.nativelap.heartguard.data.profile.remote.WorkerProfileRemoteDataSource
import com.nativelap.heartguard.domain.profile.model.WorkerProfile
import com.nativelap.heartguard.domain.profile.repository.WorkerProfileRepository
import javax.inject.Inject

class WorkerProfileRepositoryImpl @Inject constructor(
    private val workerProfileRemoteDataSource: WorkerProfileRemoteDataSource,
) : WorkerProfileRepository {
    override suspend fun getWorkerProfile(): ApiResult<WorkerProfile> = workerProfileRemoteDataSource
        .getWorkerProfile()
        .map { profileResponse -> profileResponse.toDomain() }

    override suspend fun updateWorkerName(name: String): ApiResult<WorkerProfile> = workerProfileRemoteDataSource
        .updateWorkerName(name)
        .map { profileResponse -> profileResponse.toDomain() }
}
