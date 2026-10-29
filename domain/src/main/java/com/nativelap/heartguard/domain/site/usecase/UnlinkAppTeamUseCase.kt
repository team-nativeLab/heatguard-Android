package com.nativelap.heartguard.domain.site.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.site.repository.AppSiteLinkRepository
import javax.inject.Inject

class UnlinkAppTeamUseCase
    @Inject
    constructor(
        private val appSiteLinkRepository: AppSiteLinkRepository,
    ) {
        // 팀 현장 연결을 해제한다.
        suspend operator fun invoke(
            teamId: String,
            siteId: String,
            expectedSessionGeneration: Long,
        ): ApiResult<Unit> =
            appSiteLinkRepository.unlinkTeam(
                teamId,
                siteId,
                expectedSessionGeneration,
            )
    }
