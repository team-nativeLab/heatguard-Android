package com.nativelap.heartguard.domain.site.usecase

import com.nativelap.heartguard.domain.site.repository.TeamSiteRepository
import java.time.Instant
import javax.inject.Inject

class PruneExpiredCurrentLocationWeatherCacheUseCase
    @Inject
    constructor(
        private val teamSiteRepository: TeamSiteRepository,
    ) {
        suspend operator fun invoke(now: Instant) =
            teamSiteRepository.pruneExpiredCurrentLocationWeatherCache(now)
    }
