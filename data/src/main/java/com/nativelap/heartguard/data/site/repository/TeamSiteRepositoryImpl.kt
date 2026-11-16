package com.nativelap.heartguard.data.site.repository

import android.util.Log
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.map
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherRequestDto
import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherResponseDto
import com.nativelap.heartguard.data.site.local.CachedCurrentLocationWeather
import com.nativelap.heartguard.data.site.local.CurrentLocationWeatherCache
import com.nativelap.heartguard.data.site.local.CurrentLocationWeatherSnapshot
import com.nativelap.heartguard.data.site.local.expireWeatherParts
import com.nativelap.heartguard.data.site.local.hasFreshWeatherPart
import com.nativelap.heartguard.data.site.local.hasMainWeatherValue
import com.nativelap.heartguard.data.site.local.isOwnedByCurrentSession
import com.nativelap.heartguard.data.site.local.mergeFreshWeather
import com.nativelap.heartguard.data.site.local.runCacheOperationSafely
import com.nativelap.heartguard.data.site.local.toWeatherSnapshot
import com.nativelap.heartguard.data.site.mapper.toDomain
import com.nativelap.heartguard.data.site.remote.TeamSiteRemoteDataSource
import com.nativelap.heartguard.domain.site.model.CurrentLocationWeather
import com.nativelap.heartguard.domain.site.model.TeamSiteOverview
import com.nativelap.heartguard.domain.site.model.WeatherAvailability
import com.nativelap.heartguard.domain.site.model.WeatherField
import com.nativelap.heartguard.domain.site.model.isFreshWeatherTimestamp
import com.nativelap.heartguard.domain.site.model.toWeatherInstantOrNull
import com.nativelap.heartguard.domain.site.repository.TeamSiteRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

class TeamSiteRepositoryImpl
    @Inject
    constructor(
        private val teamSiteRemoteDataSource: TeamSiteRemoteDataSource,
        private val weatherCache: CurrentLocationWeatherCache,
        private val sessionManager: SessionManager,
        private val clock: Clock,
    ) : TeamSiteRepository {
        override suspend fun getTeamSiteOverview(): ApiResult<TeamSiteOverview> =
            teamSiteRemoteDataSource.getTeamSite().map { it.toDomain() }

        override suspend fun getCurrentLocationWeather(
            latitude: Double,
            longitude: Double,
        ): ApiResult<CurrentLocationWeather> {
            val sessionSnapshot = sessionManager.getSnapshot()
            val response =
                teamSiteRemoteDataSource.getCurrentLocationWeather(
                    CurrentLocationWeatherRequestDto(latitude = latitude, longitude = longitude),
                )
            if (response !is ApiResult.Success) {
                return response.map { it.toDomain() }
            }
            if (response.value.availability !in CACHED_WEATHER_STATUSES) {
                val cachedWeather =
                    cacheOperationOrNull {
                        val owner = sessionSnapshot.sessionIdentifier ?: return@cacheOperationOrNull null
                        weatherCache.withCacheLock cache@{
                            if (sessionManager.getSnapshot() != sessionSnapshot) return@cache null
                            readFreshCachedWeather(this, clock.instant(), owner)
                        }
                    }
                return ApiResult.Success(
                    resolveUnavailableWeather(
                        incoming = response.value,
                        cached = cachedWeather,
                        requestedLatitude = latitude,
                        requestedLongitude = longitude,
                        currentSessionOwner = sessionSnapshot.sessionIdentifier,
                        isRequestSessionCurrent = sessionManager.getSnapshot() == sessionSnapshot,
                        now = clock.instant(),
                    ),
                )
            }

            val responseWeather =
                if (response.value.isStale == true && response.value.availability == "AVAILABLE") {
                    response.value.copy(availability = "CACHED")
                } else {
                    response.value
                }
            val selectedWeather =
                cacheOperationOrNull {
                    weatherCache.withCacheLock cache@{
                        val now = clock.instant()
                        val currentSnapshot = sessionManager.getSnapshot()
                        val currentOwner = currentSnapshot.sessionIdentifier
                        val previous = readFreshCachedWeather(this, now, currentOwner)
                        val incoming = responseWeather.copy(isStale = null)
                        if (sessionManager.getSnapshot() != sessionSnapshot) {
                            return@cache incoming.toWeatherSnapshot().expireWeatherParts(now)
                        }

                        val selection = selectWeatherForCache(incoming, previous, latitude, longitude, now)
                        val merged = selection.weather

                        if (merged.hasFreshWeatherPart(now)) {
                            val ownerAtWrite = sessionManager.getSnapshot()
                            if (ownerAtWrite == sessionSnapshot) {
                                ownerAtWrite.sessionIdentifier?.let { owner ->
                                    save(
                                        selection.latitude,
                                        selection.longitude,
                                        merged.weather,
                                        owner,
                                        merged.fieldProvenance,
                                    )
                                }
                            }
                        }
                        merged
                    }
                }
            return ApiResult.Success(
                resolveReturnedWeather(responseWeather, selectedWeather, clock.instant()),
            )
        }

        override suspend fun getCachedCurrentLocationWeather(
            latitude: Double,
            longitude: Double,
        ): CurrentLocationWeather? =
            cacheOperationOrNull {
                val snapshot = sessionManager.getSnapshot()
                val owner = snapshot.sessionIdentifier ?: return@cacheOperationOrNull null
                weatherCache.withCacheLock cache@{
                    val now = clock.instant()
                    if (sessionManager.getSnapshot() != snapshot) return@cache null
                    val cached = readFreshCachedWeather(this, now, owner) ?: return@cache null
                    if (!isWithinWeatherCacheRadius(latitude, longitude, cached.latitude, cached.longitude)) {
                        return@cache null
                    }
                    cached
                        .toWeatherSnapshot()
                        .copy(weather = cached.weather.asCachedWeather())
                        .toDomain()
                }
            }

        override suspend fun getLastCachedCurrentLocationWeather(): CurrentLocationWeather? =
            cacheOperationOrNull {
                val sessionSnapshot = sessionManager.getSnapshot()
                val owner = sessionSnapshot.sessionIdentifier ?: return@cacheOperationOrNull null
                weatherCache.withCacheLock cache@{
                    if (sessionManager.getSnapshot() != sessionSnapshot) return@cache null
                    val cached = readFreshCachedWeather(this, clock.instant(), owner)
                    resolveLastCachedWeatherForSession(
                        cached = cached,
                        currentSessionOwner = owner,
                        isSessionCurrent = sessionManager.getSnapshot() == sessionSnapshot,
                        now = clock.instant(),
                    )
                }
            }

        override suspend fun pruneExpiredCurrentLocationWeatherCache(now: Instant) {
            val snapshot = sessionManager.getSnapshot()
            val owner = snapshot.sessionIdentifier
            cacheOperationOrNull {
                weatherCache.withCacheLock {
                    if (sessionManager.getSnapshot() == snapshot) readFreshCachedWeather(this, now, owner)
                }
            }
        }

        private suspend fun readFreshCachedWeather(
            transaction: CurrentLocationWeatherCache.CacheTransaction,
            now: Instant,
            owner: String?,
        ): CachedCurrentLocationWeather? {
            val cached = transaction.read() ?: return null
            if (owner == null ||
                !isOwnedByCurrentSession(cached.sessionOwner, sessionManager.getSnapshot().sessionIdentifier)
            ) {
                transaction.clear()
                return null
            }
            val freshWeather = cached.toWeatherSnapshot().expireWeatherParts(now)
            if (!freshWeather.hasFreshWeatherPart(now)) {
                transaction.clear()
                return null
            }
            if (freshWeather.weather != cached.weather || freshWeather.fieldProvenance != cached.fieldProvenance) {
                transaction.save(
                    cached.latitude,
                    cached.longitude,
                    freshWeather.weather,
                    owner,
                    freshWeather.fieldProvenance,
                )
            }
            return cached.copy(weather = freshWeather.weather, fieldProvenance = freshWeather.fieldProvenance)
        }

        private suspend fun <T> cacheOperationOrNull(operation: suspend () -> T): T? =
            runCacheOperationSafely(
                onFailure = { exception ->
                    Log.w(TAG, "Current-location weather cache operation failed", exception)
                },
                operation = operation,
            )

        private companion object {
            const val TAG = "TeamSiteRepository"
            val CACHED_WEATHER_STATUSES = setOf("AVAILABLE", "CACHED")
        }
    }

internal data class WeatherCacheSelection(
    val weather: CurrentLocationWeatherSnapshot,
    val latitude: Double,
    val longitude: Double,
)

internal fun resolveUnavailableWeather(
    incoming: CurrentLocationWeatherResponseDto,
    cached: CachedCurrentLocationWeather?,
    requestedLatitude: Double,
    requestedLongitude: Double,
    currentSessionOwner: String?,
    isRequestSessionCurrent: Boolean,
    now: Instant,
): CurrentLocationWeather {
    val cachedSnapshot =
        cached
            ?.takeIf { candidate ->
                isRequestSessionCurrent && currentSessionOwner != null &&
                    candidate.sessionOwner == currentSessionOwner &&
                    isWithinWeatherCacheRadius(
                        requestedLatitude,
                        requestedLongitude,
                        candidate.latitude,
                        candidate.longitude,
                    )
            }?.let { candidate -> candidate.toWeatherSnapshot() }
            ?.let { snapshot -> snapshot.copy(weather = snapshot.weather.asCachedWeather()).expireWeatherParts(now) }
            ?.takeIf { snapshot -> snapshot.hasFreshWeatherPart(now) }

    return cachedSnapshot?.toDomain() ?: incoming.asUnavailableWeather(now)
}

private fun CurrentLocationWeatherResponseDto.asUnavailableWeather(now: Instant): CurrentLocationWeather =
    copy(
        temperature = null,
        humidity = null,
        apparentTemperature = null,
        heatLevel = null,
        observedAt = null,
        skyStatus = null,
        temperatureDelta = null,
        coldWaveWarning =
            coldWaveWarning?.takeIf { warning ->
                warning.availability in VALID_COLD_WAVE_STATUSES && warning.fetchedAt.isFreshWeatherTimestamp(now)
            },
    ).toDomain()

private val VALID_COLD_WAVE_STATUSES = setOf("AVAILABLE", "CACHED")

internal fun resolveLastCachedWeatherForSession(
    cached: CachedCurrentLocationWeather?,
    currentSessionOwner: String?,
    isSessionCurrent: Boolean,
    now: Instant,
): CurrentLocationWeather? =
    cached
        ?.takeIf { candidate ->
            isSessionCurrent && currentSessionOwner != null && candidate.sessionOwner == currentSessionOwner
        }?.let { candidate -> candidate.toWeatherSnapshot() }
        ?.let { snapshot -> snapshot.copy(weather = snapshot.weather.asCachedWeather()).expireWeatherParts(now) }
        ?.takeIf { snapshot -> snapshot.hasFreshWeatherPart(now) }
        ?.toDomain()

internal fun selectWeatherForCache(
    incoming: CurrentLocationWeatherResponseDto,
    previous: CachedCurrentLocationWeather?,
    latitude: Double,
    longitude: Double,
    now: Instant,
): WeatherCacheSelection {
    val nearby =
        previous?.takeIf {
            isWithinWeatherCacheRadius(latitude, longitude, it.latitude, it.longitude)
        }
    val previousSnapshot = nearby?.toWeatherSnapshot()
    val mergedWeather = incoming.mergeFreshWeather(previousSnapshot, now)
    val usesIncomingMainField =
        mergedWeather.fieldProvenance.observedAtByField.keys.any { field ->
            field != com.nativelap.heartguard.domain.site.model.WeatherField.COLD_WAVE_WARNING &&
                field !in mergedWeather.fieldProvenance.fieldsFromPreviousValue
        }
    return if (!usesIncomingMainField && nearby != null) {
        WeatherCacheSelection(mergedWeather, nearby.latitude, nearby.longitude)
    } else {
        WeatherCacheSelection(mergedWeather, latitude, longitude)
    }
}

private fun CachedCurrentLocationWeather.toWeatherSnapshot(): CurrentLocationWeatherSnapshot =
    CurrentLocationWeatherSnapshot(weather, fieldProvenance)

private fun CurrentLocationWeatherResponseDto.asCachedWeather(): CurrentLocationWeatherResponseDto =
    copy(
        availability = "CACHED",
        coldWaveWarning = coldWaveWarning?.copy(availability = "CACHED", isStale = true),
    )

internal fun isWithinWeatherCacheRadius(
    firstLatitude: Double,
    firstLongitude: Double,
    secondLatitude: Double,
    secondLongitude: Double,
): Boolean {
    val latitudeDelta = Math.toRadians(secondLatitude - firstLatitude)
    val longitudeDelta = Math.toRadians(secondLongitude - firstLongitude)
    val haversine =
        kotlin.math.sin(latitudeDelta / 2).let { it * it } +
            kotlin.math.cos(Math.toRadians(firstLatitude)) *
            kotlin.math.cos(Math.toRadians(secondLatitude)) *
            kotlin.math.sin(longitudeDelta / 2).let { it * it }
    val distanceMeters = 6_371_000.0 * 2 * kotlin.math.asin(kotlin.math.sqrt(haversine.coerceIn(0.0, 1.0)))
    return distanceMeters <= 1_000.0
}

internal fun CurrentLocationWeatherResponseDto.toDisplayableLiveWeather(now: Instant): CurrentLocationWeather? {
    val observation = observedAt.toWeatherInstantOrNull() ?: return null
    if (availability != "AVAILABLE" || isStale == true || observation.isAfter(now) || !hasMainWeatherValue()) {
        return null
    }
    return toWeatherSnapshot().toDomain().copy(serverReceivedAt = now.toString())
}

internal fun resolveReturnedWeather(
    incoming: CurrentLocationWeatherResponseDto,
    selected: CurrentLocationWeatherSnapshot?,
    now: Instant,
): CurrentLocationWeather {
    val liveWeather = incoming.toDisplayableLiveWeather(now)
    val selectedWeather = selected ?: incoming.toWeatherSnapshot().expireWeatherParts(now)
    if (liveWeather == null) {
        return selectedWeather.toDomain()
    }
    val selectedDomain = selectedWeather.toDomain()
    val provenanceByField = selectedDomain.fieldProvenance.observedAtByField.toMutableMap()
    val previousFields = selectedDomain.fieldProvenance.fieldsFromPreviousValue.toMutableSet()

    fun <T> chooseField(
        field: WeatherField,
        selected: T?,
        live: T?,
    ): T? {
        if (selected != null) return selected
        if (live != null) {
            liveWeather.fieldProvenance.observedAtByField[field]?.let { timestamp ->
                provenanceByField[field] =
                    timestamp
            }
            previousFields.remove(field)
            return live
        }
        return null
    }

    val temperature =
        chooseField(WeatherField.TEMPERATURE, selectedDomain.temperature, liveWeather.temperature)
    val humidity = chooseField(WeatherField.HUMIDITY, selectedDomain.humidity, liveWeather.humidity)
    val apparentTemperature =
        chooseField(
            WeatherField.APPARENT_TEMPERATURE,
            selectedDomain.apparentTemperature,
            liveWeather.apparentTemperature,
        )
    val heatLevel = chooseField(WeatherField.HEAT_LEVEL, selectedDomain.heatLevel, liveWeather.heatLevel)
    val skyStatus = chooseField(WeatherField.SKY_STATUS, selectedDomain.skyStatus, liveWeather.skyStatus)
    val temperatureDelta =
        chooseField(WeatherField.TEMPERATURE_DELTA, selectedDomain.temperatureDelta, liveWeather.temperatureDelta)
    val selectedAvailability =
        if (selectedDomain.temperature != null || selectedDomain.humidity != null ||
            selectedDomain.apparentTemperature != null ||
            selectedDomain.heatLevel != null || selectedDomain.skyStatus != null ||
            selectedDomain.temperatureDelta != null
        ) {
            selectedDomain.availability
        } else {
            liveWeather.availability
        }
    return selectedDomain.copy(
        temperature = temperature,
        humidity = humidity,
        apparentTemperature = apparentTemperature,
        heatLevel = heatLevel,
        skyStatus = skyStatus,
        temperatureDelta = temperatureDelta,
        availability = selectedAvailability,
        serverReceivedAt = now.toString(),
        fieldProvenance =
            selectedDomain.fieldProvenance.copy(
                observedAtByField = provenanceByField,
                fieldsFromPreviousValue = previousFields,
            ),
    )
}
