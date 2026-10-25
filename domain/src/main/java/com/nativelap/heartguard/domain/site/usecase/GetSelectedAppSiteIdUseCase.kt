package com.nativelap.heartguard.domain.site.usecase

import com.nativelap.heartguard.domain.site.repository.AppSiteLinkRepository
import javax.inject.Inject

class GetSelectedAppSiteIdUseCase
    @Inject
    constructor(
        private val appSiteLinkRepository: AppSiteLinkRepository,
    ) {
        operator fun invoke(): String? = appSiteLinkRepository.getSelectedSiteId()
    }
