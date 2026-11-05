package com.nativelap.heartguard.domain.profile.usecase

import com.nativelap.heartguard.domain.profile.repository.WorkerProfileRepository
import javax.inject.Inject

class ClearWorkerProfilePhotoCacheUseCase
    @Inject
    constructor(
        private val workerProfileRepository: WorkerProfileRepository,
    ) {
        /** 세션 종료 시 해당 세션의 프로필 사진 조회 URL 캐시를 비운다. */
        operator fun invoke(sessionGeneration: Long) {
            workerProfileRepository.clearCachedProfilePhoto(sessionGeneration)
        }
    }
