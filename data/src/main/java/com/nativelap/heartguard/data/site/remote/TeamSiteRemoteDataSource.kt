package com.nativelap.heartguard.data.site.remote

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherRequestDto
import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherResponseDto
import com.nativelap.heartguard.data.site.dto.TeamSiteResponseDto

interface TeamSiteRemoteDataSource {
    suspend fun getTeamSite(): ApiResult<TeamSiteResponseDto>

    suspend fun getCurrentLocationWeather(
        request: CurrentLocationWeatherRequestDto,
    ): ApiResult<CurrentLocationWeatherResponseDto>
}
