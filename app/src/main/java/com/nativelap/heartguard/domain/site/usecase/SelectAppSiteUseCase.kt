package com.nativelap.heartguard.domain.site.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.site.model.AppSelectedSite
import com.nativelap.heartguard.domain.site.repository.AppSiteLinkRepository
import javax.inject.Inject

class SelectAppSiteUseCase
    @Inject
    constructor(
        private val appSiteLinkRepository: AppSiteLinkRepository,
    ) {
        suspend operator fun invoke(siteId: String): ApiResult<AppSelectedSite> =
            appSiteLinkRepository.selectSite(siteId)
    }
