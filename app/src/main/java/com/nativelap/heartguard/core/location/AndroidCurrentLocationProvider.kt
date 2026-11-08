package com.nativelap.heartguard.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.coroutines.resume

@OptIn(InternalCoroutinesApi::class)
class AndroidCurrentLocationProvider
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : CurrentLocationProvider {
        override suspend fun getCurrentLocation(): CurrentCoordinates? {
            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return null
            }

            val locationManager = context.getSystemService(LocationManager::class.java)
            return withTimeoutOrNull(LOCATION_TIMEOUT_MILLIS) {
                requestProviderLocation(locationManager, LocationManager.NETWORK_PROVIDER)
                    ?: requestProviderLocation(locationManager, LocationManager.GPS_PROVIDER)
            }
        }

        private suspend fun requestProviderLocation(
            locationManager: LocationManager,
            provider: String,
        ): CurrentCoordinates? {
            return try {
                if (!locationManager.isProviderEnabled(provider)) {
                    return null
                }

                withTimeoutOrNull(PROVIDER_TIMEOUT_MILLIS) {
                    suspendCancellableCoroutine { continuation ->
                        val cancellationSignal = CancellationSignal()
                        continuation.invokeOnCancellation {
                            cancellationSignal.cancel()
                        }
                        locationManager.getCurrentLocation(
                            provider,
                            cancellationSignal,
                            context.mainExecutor,
                        ) { location ->
                            val coordinates =
                                location
                                    ?.let { currentLocation ->
                                        CurrentCoordinates(
                                            latitude = currentLocation.latitude,
                                            longitude = currentLocation.longitude,
                                        )
                                    }?.takeIf(CurrentCoordinates::isValid)
                            val resumeToken = continuation.tryResume(coordinates)
                            if (resumeToken != null) {
                                continuation.completeResume(resumeToken)
                            }
                        }
                    }
                }
            } catch (exception: SecurityException) {
                null
            } catch (exception: IllegalArgumentException) {
                null
            }
        }

        private companion object {
            const val PROVIDER_TIMEOUT_MILLIS = 5_000L
            const val LOCATION_TIMEOUT_MILLIS = 10_000L
        }
    }
