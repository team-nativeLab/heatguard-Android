package com.nativelap.heartguard.domain.site.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.site.model.CurrentLocationWeather
import com.nativelap.heartguard.domain.site.repository.TeamSiteRepository
import javax.inject.Inject

/** 현재 위치 좌표의 기상 정보를 조회한다. */
class GetCurrentLocationWeatherUseCase
    @Inject
    constructor(
        private val teamSiteRepository: TeamSiteRepository,
    ) {
        suspend operator fun invoke(
            latitude: Double,
            longitude: Double,
        ): ApiResult<CurrentLocationWeather> =
            teamSiteRepository.getCurrentLocationWeather(
                latitude = latitude,
                longitude = longitude,
            )
    }
