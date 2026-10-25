package com.nativelap.heartguard.domain.site.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.site.model.AppSiteLinkClaim
import com.nativelap.heartguard.domain.site.repository.AppSiteLinkRepository
import javax.inject.Inject

class ClaimAppSiteLinkUseCase
    @Inject
    constructor(
        private val appSiteLinkRepository: AppSiteLinkRepository,
    ) {
        suspend operator fun invoke(
            token: String,
            managerPhone: String,
            expectedSessionGeneration: Long,
        ): ApiResult<AppSiteLinkClaim> =
            appSiteLinkRepository.claim(
                token = token,
                managerPhone = managerPhone,
                expectedSessionGeneration = expectedSessionGeneration,
            )
    }
