package com.nativelap.heartguard.data.site.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherRequestDto
import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface TeamWeatherApiService {
    @POST("api/v1/team/weather")
    suspend fun getCurrentLocationWeather(
        @Body request: CurrentLocationWeatherRequestDto,
    ): ApiEnvelope<CurrentLocationWeatherResponseDto>
}
