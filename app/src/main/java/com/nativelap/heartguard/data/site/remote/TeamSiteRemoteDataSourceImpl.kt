package com.nativelap.heartguard.data.site.remote

import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.TeamTokenProvider
import com.nativelap.heartguard.data.site.dto.TeamSiteResponseDto
import javax.inject.Inject

class TeamSiteRemoteDataSourceImpl @Inject constructor(
    private val teamSiteApiService: TeamSiteApiService,
    private val teamTokenProvider: TeamTokenProvider,
    private val apiExecutor: ApiExecutor,
) : TeamSiteRemoteDataSource {

    override suspend fun getTeamSite(): ApiResult<TeamSiteResponseDto> = apiExecutor.execute {
        val envelope = teamSiteApiService.getTeamSite(teamTokenProvider.currentTeamToken())
        envelope.data ?: error("팀 현장페이지 응답에 data가 없습니다.")
    }
}
