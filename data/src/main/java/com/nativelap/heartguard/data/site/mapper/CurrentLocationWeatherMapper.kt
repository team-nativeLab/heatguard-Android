package com.nativelap.heartguard.data.site.mapper

import com.nativelap.heartguard.data.site.dto.ColdWaveWarningDto
import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherResponseDto
import com.nativelap.heartguard.domain.site.model.ColdWaveAvailability
import com.nativelap.heartguard.domain.site.model.ColdWaveLevel
import com.nativelap.heartguard.domain.site.model.ColdWaveWarning
import com.nativelap.heartguard.domain.site.model.CurrentLocationWeather
import com.nativelap.heartguard.domain.site.model.SkyStatus
import com.nativelap.heartguard.domain.site.model.WeatherAvailability
import java.util.Locale

internal fun CurrentLocationWeatherResponseDto.toDomain(): CurrentLocationWeather =
    CurrentLocationWeather(
        temperature = temperature,
        humidity = humidity,
        apparentTemperature = apparentTemperature,
        heatLevel = heatLevel,
        observedAt = observedAt,
        skyStatus = skyStatus.toSkyStatus(),
        temperatureDelta = temperatureDelta,
        availability = availability.toWeatherAvailability(),
        coldWaveWarning = coldWaveWarning?.toDomain(),
    )

internal fun ColdWaveWarningDto.toDomain(): ColdWaveWarning =
    ColdWaveWarning(
        level = level.toColdWaveLevel(),
        availability = availability.toColdWaveAvailability(),
        areaCode = areaCode,
        areaName = areaName,
        issuedAt = issuedAt,
        effectiveAt = effectiveAt,
        fetchedAt = fetchedAt,
        isStale = isStale,
        errorCode = errorCode,
        source = source,
    )

private fun String?.toColdWaveLevel(): ColdWaveLevel? =
    when (this?.uppercase(Locale.ROOT)) {
        "NONE" -> ColdWaveLevel.NONE
        "ADVISORY" -> ColdWaveLevel.ADVISORY
        "WARNING" -> ColdWaveLevel.WARNING
        null, "" -> null
        else -> ColdWaveLevel.UNKNOWN
    }

private fun String?.toColdWaveAvailability(): ColdWaveAvailability? =
    when (this?.uppercase(Locale.ROOT)) {
        "AVAILABLE" -> ColdWaveAvailability.AVAILABLE
        "CACHED" -> ColdWaveAvailability.CACHED
        "UNAVAILABLE" -> ColdWaveAvailability.UNAVAILABLE
        "LOCATION_REQUIRED" -> ColdWaveAvailability.LOCATION_REQUIRED
        null, "" -> null
        else -> ColdWaveAvailability.UNKNOWN
    }

private fun String?.toSkyStatus(): SkyStatus? =
    when (this?.uppercase(Locale.ROOT)) {
        "CLEAR" -> SkyStatus.CLEAR
        "PARTLY_CLOUDY" -> SkyStatus.PARTLY_CLOUDY
        "CLOUDY" -> SkyStatus.CLOUDY
        "RAIN" -> SkyStatus.RAIN
        "SNOW" -> SkyStatus.SNOW
        null, "" -> null
        else -> SkyStatus.UNKNOWN
    }

private fun String?.toWeatherAvailability(): WeatherAvailability =
    when (this?.uppercase(Locale.ROOT)) {
        "AVAILABLE" -> WeatherAvailability.AVAILABLE
        "CACHED" -> WeatherAvailability.CACHED
        "UNAVAILABLE" -> WeatherAvailability.UNAVAILABLE
        else -> WeatherAvailability.UNKNOWN
    }
