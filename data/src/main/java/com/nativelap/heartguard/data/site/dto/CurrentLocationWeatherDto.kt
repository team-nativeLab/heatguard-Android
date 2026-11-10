package com.nativelap.heartguard.data.site.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CurrentLocationWeatherRequestDto(
    @SerialName("latitude")
    val latitude: Double,
    @SerialName("longitude")
    val longitude: Double,
)

@Serializable
data class CurrentLocationWeatherResponseDto(
    @SerialName("temperature")
    val temperature: Double? = null,
    @SerialName("humidity")
    val humidity: Double? = null,
    @SerialName("apparentTemperature")
    val apparentTemperature: Double? = null,
    @SerialName("heatLevel")
    val heatLevel: Int? = null,
    @SerialName("observedAt")
    val observedAt: String? = null,
    @SerialName("skyStatus")
    val skyStatus: String? = null,
    @SerialName("temperatureDelta")
    val temperatureDelta: Double? = null,
    @SerialName("availability")
    val availability: String? = null,
    @SerialName("errorCode")
    val errorCode: String? = null,
    @SerialName("coldWaveWarning")
    val coldWaveWarning: ColdWaveWarningDto? = null,
)
