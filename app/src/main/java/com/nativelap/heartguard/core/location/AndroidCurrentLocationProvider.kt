package com.nativelap.heartguard.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CancellationException
import javax.inject.Inject

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

            val fusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(context)
            val cancellationTokenSource = CancellationTokenSource()

            return try {
                val currentLocation =
                    withTimeoutOrNull(LOCATION_TIMEOUT_MILLIS) {
                        awaitCurrentLocation(
                            fusedLocationProviderClient,
                            cancellationTokenSource,
                        )
                    }
                currentLocation.toCurrentCoordinates()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                null
            } finally {
                cancellationTokenSource.cancel()
            }
        }

        private suspend fun awaitCurrentLocation(
            fusedLocationProviderClient: FusedLocationProviderClient,
            cancellationTokenSource: CancellationTokenSource,
        ): Location? =
            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                }

                try {
                    fusedLocationProviderClient
                        .getCurrentLocation(
                            CURRENT_LOCATION_REQUEST,
                            cancellationTokenSource.token,
                        ).addOnCompleteListener(context.mainExecutor) { locationTask ->
                            if (locationTask.isSuccessful) {
                                val resumeToken = continuation.tryResume(locationTask.result)
                                if (resumeToken != null) {
                                    continuation.completeResume(resumeToken)
                                }
                            } else {
                                val taskException = locationTask.exception
                                val resumeToken =
                                    if (taskException == null) {
                                        continuation.tryResume(null)
                                    } else {
                                        continuation.tryResumeWithException(taskException)
                                    }
                                if (resumeToken != null) {
                                    continuation.completeResume(resumeToken)
                                }
                            }
                        }
                } catch (exception: Exception) {
                    val resumeToken = continuation.tryResumeWithException(exception)
                    if (resumeToken != null) {
                        continuation.completeResume(resumeToken)
                    }
                }
            }

        private fun Location?.toCurrentCoordinates(): CurrentCoordinates? =
            this
                ?.let { currentLocation ->
                    CurrentCoordinates(
                        latitude = currentLocation.latitude,
                        longitude = currentLocation.longitude,
                    )
                }?.takeIf(CurrentCoordinates::isValid)

        private companion object {
            const val MAX_UPDATE_AGE_MILLIS = 0L
            const val FUSED_REQUEST_DURATION_MILLIS = 10_000L
            const val LOCATION_TIMEOUT_MILLIS = 12_000L

            val CURRENT_LOCATION_REQUEST =
                CurrentLocationRequest
                    .Builder()
                    .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                    .setGranularity(Granularity.GRANULARITY_COARSE)
                    .setMaxUpdateAgeMillis(MAX_UPDATE_AGE_MILLIS)
                    .setDurationMillis(FUSED_REQUEST_DURATION_MILLIS)
                    .build()
        }
    }
