package com.nativelap.heartguard.domain.site.model

/** 기상청 한파 특보 조회 결과다. */
data class ColdWaveWarning(
    val level: ColdWaveLevel?,
    val availability: ColdWaveAvailability?,
    val areaCode: String?,
    val areaName: String?,
    val issuedAt: String?,
    val effectiveAt: String?,
    val fetchedAt: String?,
    val isStale: Boolean?,
    val errorCode: String?,
    val source: String?,
)

enum class ColdWaveLevel {
    NONE,
    ADVISORY,
    WARNING,
    UNKNOWN,
}

enum class ColdWaveAvailability {
    AVAILABLE,
    CACHED,
    UNAVAILABLE,
    LOCATION_REQUIRED,
    UNKNOWN,
}
