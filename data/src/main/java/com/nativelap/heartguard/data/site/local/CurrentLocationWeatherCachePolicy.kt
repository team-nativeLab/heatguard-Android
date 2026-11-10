package com.nativelap.heartguard.data.site.local

import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherResponseDto
import com.nativelap.heartguard.domain.site.model.isFreshWeatherTimestamp
import java.time.Instant

internal fun CurrentLocationWeatherResponseDto.expireWeatherParts(now: Instant): CurrentLocationWeatherResponseDto {
    val isMainWeatherFresh = observedAt.isFreshWeatherTimestamp(now)
    val freshColdWaveWarning = coldWaveWarning?.takeIf { warning -> warning.fetchedAt.isFreshWeatherTimestamp(now) }
    val hasFreshData = isMainWeatherFresh || freshColdWaveWarning != null
    val sanitizedAvailability =
        when {
            !hasFreshData -> "UNAVAILABLE"
            availability == "CACHED" || !isMainWeatherFresh -> "CACHED"
            else -> "AVAILABLE"
        }
    return copy(
        temperature = temperature.takeIf { isMainWeatherFresh },
        humidity = humidity.takeIf { isMainWeatherFresh },
        apparentTemperature = apparentTemperature.takeIf { isMainWeatherFresh },
        heatLevel = heatLevel.takeIf { isMainWeatherFresh },
        observedAt = observedAt.takeIf { isMainWeatherFresh },
        skyStatus = skyStatus.takeIf { isMainWeatherFresh },
        temperatureDelta = temperatureDelta.takeIf { isMainWeatherFresh },
        availability = sanitizedAvailability,
        coldWaveWarning = freshColdWaveWarning,
    )
}

internal fun CurrentLocationWeatherResponseDto.hasFreshWeatherPart(now: Instant): Boolean =
    observedAt.isFreshWeatherTimestamp(now) ||
        coldWaveWarning?.fetchedAt.isFreshWeatherTimestamp(now)
