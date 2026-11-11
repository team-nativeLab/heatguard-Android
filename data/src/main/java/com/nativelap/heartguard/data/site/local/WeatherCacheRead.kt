package com.nativelap.heartguard.data.site.local

import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherResponseDto
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal sealed interface WeatherCacheRead {
    data object Empty : WeatherCacheRead

    data object Invalid : WeatherCacheRead

    data class Valid(
        val cachedWeather: CachedCurrentLocationWeather,
    ) : WeatherCacheRead
}

internal fun readWeatherCacheFields(
    payload: String?,
    latitude: Double?,
    longitude: Double?,
    sessionOwner: String?,
    json: Json,
): WeatherCacheRead {
    if (payload == null && latitude == null && longitude == null && sessionOwner == null) {
        return WeatherCacheRead.Empty
    }
    if (payload.isNullOrBlank() ||
        latitude == null || !latitude.isFinite() || latitude !in -90.0..90.0 ||
        longitude == null || !longitude.isFinite() || longitude !in -180.0..180.0 ||
        sessionOwner.isNullOrBlank()
    ) {
        return WeatherCacheRead.Invalid
    }

    val weather =
        try {
            json.decodeFromString<CurrentLocationWeatherResponseDto>(payload)
        } catch (_: SerializationException) {
            return WeatherCacheRead.Invalid
        } catch (_: IllegalArgumentException) {
            return WeatherCacheRead.Invalid
        }

    return WeatherCacheRead.Valid(
        CachedCurrentLocationWeather(
            latitude = latitude,
            longitude = longitude,
            weather = weather,
            sessionOwner = sessionOwner,
        ),
    )
}

internal suspend fun WeatherCacheRead.resolveOrClearInvalid(
    clearCache: suspend () -> Unit,
): CachedCurrentLocationWeather? =
    when (this) {
        WeatherCacheRead.Empty -> {
            null
        }

        WeatherCacheRead.Invalid -> {
            clearCache()
            null
        }

        is WeatherCacheRead.Valid -> {
            cachedWeather
        }
    }
