package com.nativelap.heartguard.data.site.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ColdWaveWarningDto(
    @SerialName("level")
    val level: String? = null,
    @SerialName("availability")
    val availability: String? = null,
    @SerialName("areaCode")
    val areaCode: String? = null,
    @SerialName("areaName")
    val areaName: String? = null,
    @SerialName("issuedAt")
    val issuedAt: String? = null,
    @SerialName("effectiveAt")
    val effectiveAt: String? = null,
    @SerialName("fetchedAt")
    val fetchedAt: String? = null,
    @SerialName("isStale")
    val isStale: Boolean? = null,
    @SerialName("errorCode")
    val errorCode: String? = null,
    @SerialName("source")
    val source: String? = null,
)
