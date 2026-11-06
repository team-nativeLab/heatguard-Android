package com.nativelap.heartguard.data.site.mapper

import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherResponseDto
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
    )

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
