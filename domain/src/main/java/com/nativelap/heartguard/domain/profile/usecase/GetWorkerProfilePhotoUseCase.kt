package com.nativelap.heartguard.domain.profile.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.profile.model.ProfilePhoto
import com.nativelap.heartguard.domain.profile.repository.WorkerProfileRepository
import javax.inject.Inject

class GetWorkerProfilePhotoUseCase
    @Inject
    constructor(
        private val workerProfileRepository: WorkerProfileRepository,
    ) {
        suspend operator fun invoke(expectedSessionGeneration: Long): ApiResult<ProfilePhoto?> =
            workerProfileRepository.getProfilePhoto(expectedSessionGeneration)
    }
