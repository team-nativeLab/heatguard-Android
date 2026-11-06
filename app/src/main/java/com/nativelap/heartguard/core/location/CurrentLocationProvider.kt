package com.nativelap.heartguard.core.location

interface CurrentLocationProvider {
    suspend fun getCurrentLocation(): CurrentCoordinates?
}
