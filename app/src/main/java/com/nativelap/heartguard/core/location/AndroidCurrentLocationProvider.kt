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

            return try {
                val locationManager = context.getSystemService(LocationManager::class.java)
                if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    return null
                }

                suspendCancellableCoroutine { continuation ->
                    val cancellationSignal = CancellationSignal()
                    continuation.invokeOnCancellation {
                        cancellationSignal.cancel()
                    }
                    locationManager.getCurrentLocation(
                        LocationManager.NETWORK_PROVIDER,
                        cancellationSignal,
                        context.mainExecutor,
                    ) { location ->
                        val coordinates =
                            location?.let { currentLocation ->
                                CurrentCoordinates(
                                    latitude = currentLocation.latitude,
                                    longitude = currentLocation.longitude,
                                )
                            }
                        val resumeToken = continuation.tryResume(coordinates)
                        if (resumeToken != null) {
                            continuation.completeResume(resumeToken)
                        }
                    }
                }
            } catch (exception: SecurityException) {
                null
            } catch (exception: IllegalArgumentException) {
                null
            }
        }
    }
