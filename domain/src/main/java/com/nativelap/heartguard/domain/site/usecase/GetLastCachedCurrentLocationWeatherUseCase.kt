package com.nativelap.heartguard.domain.site.usecase

import com.nativelap.heartguard.domain.site.model.CurrentLocationWeather
import com.nativelap.heartguard.domain.site.repository.TeamSiteRepository
import javax.inject.Inject

class GetLastCachedCurrentLocationWeatherUseCase
    @Inject
    constructor(
        private val teamSiteRepository: TeamSiteRepository,
    ) {
        suspend operator fun invoke(): CurrentLocationWeather? =
            teamSiteRepository.getLastCachedCurrentLocationWeather()
    }
