package com.nativelap.heartguard.data.site.remote

import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.requireSuccessData
import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherRequestDto
import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherResponseDto
import com.nativelap.heartguard.data.site.dto.TeamSiteResponseDto
import javax.inject.Inject

class TeamSiteRemoteDataSourceImpl
    @Inject
    constructor(
        private val teamSiteApiService: TeamSiteApiService,
        private val apiExecutor: ApiExecutor,
    ) : TeamSiteRemoteDataSource {
        override suspend fun getTeamSite(): ApiResult<TeamSiteResponseDto> =
            apiExecutor
                .execute {
                    teamSiteApiService.getTeamSite()
                }.requireSuccessData()

        override suspend fun getCurrentLocationWeather(
            request: CurrentLocationWeatherRequestDto,
        ): ApiResult<CurrentLocationWeatherResponseDto> =
            apiExecutor
                .execute {
                    teamSiteApiService.getCurrentLocationWeather(request)
                }.requireSuccessData()
    }
