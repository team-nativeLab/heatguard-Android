package com.nativelap.heartguard.domain.site.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.site.repository.AppSiteLinkRepository
import javax.inject.Inject

class UnlinkAppSiteUseCase
    @Inject
    constructor(
        private val appSiteLinkRepository: AppSiteLinkRepository,
    ) {
        suspend operator fun invoke(
            siteId: String,
            expectedSessionGeneration: Long,
        ): ApiResult<Unit> = appSiteLinkRepository.unlinkSite(siteId, expectedSessionGeneration)
    }
