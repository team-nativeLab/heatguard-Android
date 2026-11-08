package com.nativelap.heartguard.domain.site.model

/** 현재 좌표에 대해 서버가 반환한 기상 값이다. */
data class CurrentLocationWeather(
    val temperature: Double?,
    val humidity: Double?,
    val apparentTemperature: Double?,
    val heatLevel: Int?,
    val observedAt: String?,
    val skyStatus: SkyStatus?,
    val temperatureDelta: Double?,
    val availability: WeatherAvailability,
    val coldWaveWarning: ColdWaveWarning? = null,
)
