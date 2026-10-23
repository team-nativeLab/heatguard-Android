package com.nativelap.heartguard.domain.site.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.site.model.AppLinkedSite
import com.nativelap.heartguard.domain.site.repository.AppSiteLinkRepository
import javax.inject.Inject

class GetAppLinkedSitesUseCase
    @Inject
    constructor(
        private val appSiteLinkRepository: AppSiteLinkRepository,
    ) {
        suspend operator fun invoke(): ApiResult<List<AppLinkedSite>> = appSiteLinkRepository.getLinkedSites()
    }
