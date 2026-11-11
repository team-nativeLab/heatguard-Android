package com.nativelap.heartguard.domain.site.model

import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime

private const val WEATHER_CACHE_MAX_AGE_SECONDS = 60 * 60L

fun String?.toWeatherInstantOrNull(): Instant? =
    runCatching { this?.let(OffsetDateTime::parse)?.toInstant() }.getOrNull()

fun String?.isFreshWeatherTimestamp(now: Instant): Boolean =
    toWeatherInstantOrNull()?.let { timestamp ->
        !timestamp.isAfter(now) && timestamp.isAfter(now.minusSeconds(WEATHER_CACHE_MAX_AGE_SECONDS))
    } ?: false

fun String?.elapsedWeatherMinutes(now: Instant): Long? =
    toWeatherInstantOrNull()
        ?.takeIf { timestamp -> !timestamp.isAfter(now) }
        ?.let { timestamp -> Duration.between(timestamp, now).toMinutes() }
