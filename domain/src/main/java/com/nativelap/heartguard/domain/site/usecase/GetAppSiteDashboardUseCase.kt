package com.nativelap.heartguard.domain.site.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.site.model.AppSiteDashboard
import com.nativelap.heartguard.domain.site.repository.AppSiteLinkRepository
import javax.inject.Inject

class GetAppSiteDashboardUseCase
    @Inject
    constructor(
        private val appSiteLinkRepository: AppSiteLinkRepository,
    ) {
        suspend operator fun invoke(
            siteId: String,
            expectedSessionGeneration: Long,
        ): ApiResult<AppSiteDashboard> = appSiteLinkRepository.getDashboard(siteId, expectedSessionGeneration)
    }
