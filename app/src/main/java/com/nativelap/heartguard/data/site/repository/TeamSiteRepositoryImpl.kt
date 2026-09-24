package com.nativelap.heartguard.data.site.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.site.mapper.toDomain
import com.nativelap.heartguard.data.site.remote.TeamSiteRemoteDataSource
import com.nativelap.heartguard.domain.site.model.TeamSiteOverview
import com.nativelap.heartguard.domain.site.repository.TeamSiteRepository
import javax.inject.Inject

class TeamSiteRepositoryImpl @Inject constructor(
    private val teamSiteRemoteDataSource: TeamSiteRemoteDataSource,
) : TeamSiteRepository {

    override suspend fun getTeamSiteOverview(): ApiResult<TeamSiteOverview> =
        when (val result = teamSiteRemoteDataSource.getTeamSite()) {
            is ApiResult.Success -> ApiResult.Success(result.value.toDomain())
            is ApiResult.Failure -> result
        }
}
