package com.nativelap.heartguard.domain.site.usecase

import com.nativelap.heartguard.domain.site.model.CurrentLocationWeather
import com.nativelap.heartguard.domain.site.repository.TeamSiteRepository
import javax.inject.Inject

class GetCachedCurrentLocationWeatherUseCase
    @Inject
    constructor(
        private val teamSiteRepository: TeamSiteRepository,
    ) {
        suspend operator fun invoke(latitude: Double, longitude: Double): CurrentLocationWeather? =
            teamSiteRepository.getCachedCurrentLocationWeather(latitude, longitude)
    }
