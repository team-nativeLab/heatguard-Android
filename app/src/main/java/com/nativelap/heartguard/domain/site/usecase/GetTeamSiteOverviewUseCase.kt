package com.nativelap.heartguard.domain.site.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.site.model.TeamSiteOverview
import com.nativelap.heartguard.domain.site.repository.TeamSiteRepository
import javax.inject.Inject

/** 홈 화면 진입 시 팀 현장페이지 정보를 조회한다. */
class GetTeamSiteOverviewUseCase @Inject constructor(
    private val teamSiteRepository: TeamSiteRepository,
) {
    suspend operator fun invoke(): ApiResult<TeamSiteOverview> = teamSiteRepository.getTeamSiteOverview()
}
