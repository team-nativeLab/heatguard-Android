package com.nativelap.heartguard.viewmodel.home

import com.nativelap.heartguard.domain.site.model.toWeatherInstantOrNull
import java.time.Duration
import java.time.Instant

internal fun nextWeatherTimestampRefresh(
    now: Instant,
    serverTimestamps: List<String?>,
): Instant? =
    serverTimestamps
        .mapNotNull { timestamp -> timestamp.nextWeatherTimestampRefresh(now) }
        .minOrNull()

private fun String?.nextWeatherTimestampRefresh(now: Instant): Instant? {
    val observedAt = toWeatherInstantOrNull() ?: return null
    if (observedAt.isAfter(now)) return null

    val expiration = observedAt.plusSeconds(WEATHER_MAX_AGE_SECONDS)
    if (!expiration.isAfter(now)) return null

    val elapsedMinutes = Duration.between(observedAt, now).toMinutes()
    val nextAgeBoundary = observedAt.plusSeconds((elapsedMinutes + 1L) * SECONDS_PER_MINUTE)
    return minOf(nextAgeBoundary, expiration)
}

private const val WEATHER_MAX_AGE_SECONDS = 3_600L
private const val SECONDS_PER_MINUTE = 60L
