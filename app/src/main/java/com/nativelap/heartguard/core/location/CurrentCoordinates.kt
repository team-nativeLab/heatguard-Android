package com.nativelap.heartguard.core.location

data class CurrentCoordinates(
    val latitude: Double,
    val longitude: Double,
) {
    val isValid: Boolean
        get() =
            latitude.isFinite() &&
                longitude.isFinite() &&
                latitude in MIN_LATITUDE..MAX_LATITUDE &&
                longitude in MIN_LONGITUDE..MAX_LONGITUDE

    private companion object {
        const val MIN_LATITUDE = -90.0
        const val MAX_LATITUDE = 90.0
        const val MIN_LONGITUDE = -180.0
        const val MAX_LONGITUDE = 180.0
    }
}
