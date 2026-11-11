package com.nativelap.heartguard.data.site.repository

import android.util.Log
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.map
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherRequestDto
import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherResponseDto
import com.nativelap.heartguard.data.site.local.CachedCurrentLocationWeather
import com.nativelap.heartguard.data.site.local.CurrentLocationWeatherCache
import com.nativelap.heartguard.data.site.local.expireWeatherParts
import com.nativelap.heartguard.data.site.local.hasFreshWeatherPart
import com.nativelap.heartguard.data.site.local.isOwnedByCurrentSession
import com.nativelap.heartguard.data.site.local.runCacheOperationSafely
import com.nativelap.heartguard.data.site.mapper.toDomain
import com.nativelap.heartguard.data.site.remote.TeamSiteRemoteDataSource
import com.nativelap.heartguard.domain.site.model.CurrentLocationWeather
import com.nativelap.heartguard.domain.site.model.TeamSiteOverview
import com.nativelap.heartguard.domain.site.model.WeatherAvailability
import com.nativelap.heartguard.domain.site.repository.TeamSiteRepository
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
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
            if (response !is ApiResult.Success || response.value.availability !in CACHED_WEATHER_STATUSES) {
                return response.map { it.toDomain() }
            }

            val selectedWeather =
                cacheOperationOrNull {
                    weatherCache.withCacheLock cache@{
                        val now = clock.instant()
                        val currentSnapshot = sessionManager.getSnapshot()
                        val currentOwner = currentSnapshot.sessionIdentifier
                        val previous = readFreshCachedWeather(this, now, currentOwner)
                        val incoming = response.value.expireWeatherParts(now)
                        if (sessionManager.getSnapshot() != sessionSnapshot) {
                            return@cache incoming
                        }

                        val incomingTime = response.value.observedAt.toServerInstant()
                        val previousTime = previous?.weather?.observedAt.toServerInstant()
                        if (previous != null && incomingTime != null && previousTime != null &&
                            incomingTime.isBefore(previousTime) &&
                            haversineDistanceMeters(latitude, longitude, previous.latitude, previous.longitude) <=
                            CACHE_LOCATION_RADIUS_METERS
                        ) {
                            return@cache previous.weather.asCachedWeather()
                        }

                        if (incoming.hasFreshWeatherPart(now)) {
                            val ownerAtWrite = sessionManager.getSnapshot()
                            if (ownerAtWrite == sessionSnapshot) {
                                ownerAtWrite.sessionIdentifier?.let { owner ->
                                    save(latitude, longitude, incoming, owner)
                                }
                            }
                        }
                        incoming
                    }
                }
            return ApiResult.Success((selectedWeather ?: response.value.expireWeatherParts(clock.instant())).toDomain())
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
                    if (haversineDistanceMeters(latitude, longitude, cached.latitude, cached.longitude) >
                        CACHE_LOCATION_RADIUS_METERS
                    ) {
                        return@cache null
                    }
                    cached.weather.asCachedWeather().toDomain()
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
            val freshWeather = cached.weather.expireWeatherParts(now)
            if (!freshWeather.hasFreshWeatherPart(now)) {
                transaction.clear()
                return null
            }
            if (freshWeather != cached.weather) {
                transaction.save(cached.latitude, cached.longitude, freshWeather, owner)
            }
            return cached.copy(weather = freshWeather)
        }

        private suspend fun <T> cacheOperationOrNull(operation: suspend () -> T): T? =
            runCacheOperationSafely(
                onFailure = { exception ->
                    Log.w(TAG, "Current-location weather cache operation failed", exception)
                },
                operation = operation,
            )

        private fun CurrentLocationWeatherResponseDto.asCachedWeather(): CurrentLocationWeatherResponseDto =
            copy(
                availability = "CACHED",
                coldWaveWarning =
                    coldWaveWarning?.copy(
                        availability = "CACHED",
                        isStale = true,
                    ),
            )

        private fun String?.toServerInstant(): Instant? =
            runCatching { this?.let(OffsetDateTime::parse)?.toInstant() }.getOrNull()

        private fun haversineDistanceMeters(
            firstLatitude: Double,
            firstLongitude: Double,
            secondLatitude: Double,
            secondLongitude: Double,
        ): Double {
            val latitudeDelta = Math.toRadians(secondLatitude - firstLatitude)
            val longitudeDelta = Math.toRadians(secondLongitude - firstLongitude)
            val haversine =
                kotlin.math.sin(latitudeDelta / 2).let { it * it } +
                    kotlin.math.cos(Math.toRadians(firstLatitude)) *
                    kotlin.math.cos(Math.toRadians(secondLatitude)) *
                    kotlin.math.sin(longitudeDelta / 2).let { it * it }
            return 6_371_000.0 * 2 * kotlin.math.asin(kotlin.math.sqrt(haversine.coerceIn(0.0, 1.0)))
        }

        private companion object {
            const val TAG = "TeamSiteRepository"
            val CACHED_WEATHER_STATUSES = setOf("AVAILABLE", "CACHED")
            const val CACHE_LOCATION_RADIUS_METERS = 1_000.0
        }
    }
