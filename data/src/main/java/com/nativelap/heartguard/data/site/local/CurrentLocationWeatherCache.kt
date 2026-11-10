package com.nativelap.heartguard.data.site.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nativelap.heartguard.core.session.SessionScopedDataCleaner
import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherResponseDto
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.currentLocationWeatherDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "current_location_weather")

data class CachedCurrentLocationWeather(
    val latitude: Double,
    val longitude: Double,
    val weather: CurrentLocationWeatherResponseDto,
    val sessionOwner: String,
)

@Singleton
class CurrentLocationWeatherCache
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val clock: Clock,
    ) : SessionScopedDataCleaner {
        private val json = Json
        private val payloadKey = stringPreferencesKey("payload")
        private val latitudeKey = doublePreferencesKey("latitude")
        private val longitudeKey = doublePreferencesKey("longitude")
        private val sessionOwnerKey = stringPreferencesKey("session_owner")
        private val mutex = WeatherCacheLock()

        suspend fun <T> withCacheLock(block: suspend CacheTransaction.() -> T): T =
            mutex.withLock { CacheTransaction().block() }

        inner class CacheTransaction internal constructor() {
            suspend fun read(): CachedCurrentLocationWeather? =
                context.currentLocationWeatherDataStore.data
                    .first()
                    .let { preferences ->
                        val cacheRead =
                            try {
                                readWeatherCacheFields(
                                    payload = preferences[payloadKey],
                                    latitude = preferences[latitudeKey],
                                    longitude = preferences[longitudeKey],
                                    sessionOwner = preferences[sessionOwnerKey],
                                    json = json,
                                )
                            } catch (_: ClassCastException) {
                                WeatherCacheRead.Invalid
                            }
                        cacheRead.resolveOrClearInvalid { clear() }
                    }

            suspend fun save(
                latitude: Double,
                longitude: Double,
                weather: CurrentLocationWeatherResponseDto,
                sessionOwner: String,
            ) {
                context.currentLocationWeatherDataStore.edit { preferences ->
                    preferences[payloadKey] = json.encodeToString(weather)
                    preferences[latitudeKey] = latitude
                    preferences[longitudeKey] = longitude
                    preferences[sessionOwnerKey] = sessionOwner
                }
            }

            suspend fun clear() {
                context.currentLocationWeatherDataStore.edit { it.clear() }
            }
        }

        override suspend fun clearSessionData() = withCacheLock { clear() }

        override suspend fun onSessionRestored(sessionIdentifier: String) {
            withCacheLock {
                val cached = read() ?: return@withCacheLock
                if (!isOwnedByCurrentSession(cached.sessionOwner, sessionIdentifier)) {
                    clear()
                    return@withCacheLock
                }
                val now = clock.instant()
                val freshWeather = cached.weather.expireWeatherParts(now)
                if (!freshWeather.hasFreshWeatherPart(now)) {
                    clear()
                } else if (freshWeather != cached.weather) {
                    save(cached.latitude, cached.longitude, freshWeather, sessionIdentifier)
                }
            }
        }
    }
