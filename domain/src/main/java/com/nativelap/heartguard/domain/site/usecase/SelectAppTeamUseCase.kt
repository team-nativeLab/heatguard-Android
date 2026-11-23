package com.nativelap.heartguard.domain.site.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.site.model.AppSelectedSite
import com.nativelap.heartguard.domain.site.repository.AppSiteLinkRepository
import javax.inject.Inject

class SelectAppTeamUseCase
    @Inject
    constructor(
        private val appSiteLinkRepository: AppSiteLinkRepository,
    ) {
        suspend operator fun invoke(
            teamId: String,
            expectedSessionGeneration: Long,
        ): ApiResult<AppSelectedSite> =
            appSiteLinkRepository.selectTeam(
                teamId,
                expectedSessionGeneration,
            )
    }
