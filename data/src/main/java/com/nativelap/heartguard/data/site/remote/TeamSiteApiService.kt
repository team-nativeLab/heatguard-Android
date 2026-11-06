package com.nativelap.heartguard.data.site.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherRequestDto
import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherResponseDto
import com.nativelap.heartguard.data.site.dto.TeamSiteResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface TeamSiteApiService {
    @GET("api/v1/team")
    suspend fun getTeamSite(): ApiEnvelope<TeamSiteResponseDto>

    @POST("api/v1/team/weather")
    suspend fun getCurrentLocationWeather(
        @Body request: CurrentLocationWeatherRequestDto,
    ): ApiEnvelope<CurrentLocationWeatherResponseDto>
}
