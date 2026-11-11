package com.nativelap.heartguard.data.site.local

import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherResponseDto
import com.nativelap.heartguard.domain.site.model.isFreshWeatherTimestamp
import com.nativelap.heartguard.domain.site.model.toWeatherInstantOrNull
import java.time.Instant

internal fun CurrentLocationWeatherResponseDto.expireWeatherParts(now: Instant): CurrentLocationWeatherResponseDto {
    val isMainWeatherFresh = observedAt.isFreshWeatherTimestamp(now) && hasMainWeatherValue()
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
    (observedAt.isFreshWeatherTimestamp(now) && hasMainWeatherValue()) ||
        coldWaveWarning?.fetchedAt.isFreshWeatherTimestamp(now)

internal fun CurrentLocationWeatherResponseDto.hasMainWeatherValue(): Boolean =
    temperature != null || humidity != null || apparentTemperature != null ||
        heatLevel != null || skyStatus != null || temperatureDelta != null

internal fun CurrentLocationWeatherResponseDto.mergeFreshWeather(
    previous: CurrentLocationWeatherResponseDto?,
    now: Instant,
): CurrentLocationWeatherResponseDto {
    val incoming = expireWeatherParts(now)
    val old = previous?.expireWeatherParts(now)
    val incomingTime = incoming.observedAt.toWeatherInstantOrNull()
    val previousTime = old?.observedAt.toWeatherInstantOrNull()
    val keepPreviousMain =
        old?.temperature != null && old.humidity != null &&
            (
                incoming.temperature == null || incoming.humidity == null ||
                    (incomingTime != null && previousTime != null && incomingTime.isBefore(previousTime))
            )
    val previousWarning = old?.coldWaveWarning
    val incomingWarning = incoming.coldWaveWarning
    val oldWarningTime = previousWarning?.fetchedAt.toWeatherInstantOrNull()
    val incomingWarningTime = incomingWarning?.fetchedAt.toWeatherInstantOrNull()
    val keepPreviousWarning =
        previousWarning != null &&
            (
                incomingWarning == null || incomingWarning.availability !in setOf("AVAILABLE", "CACHED") ||
                    (
                        oldWarningTime != null && incomingWarningTime != null &&
                            incomingWarningTime.isBefore(oldWarningTime)
                    )
            )
    val selectedMain = if (keepPreviousMain) old!! else incoming
    val selectedWarning =
        if (keepPreviousWarning) {
            previousWarning?.copy(availability = "CACHED", isStale = true)
        } else {
            incomingWarning
        }
    return selectedMain
        .copy(
            availability = if (keepPreviousMain) "CACHED" else incoming.availability,
            coldWaveWarning = selectedWarning,
        ).expireWeatherParts(now)
}
