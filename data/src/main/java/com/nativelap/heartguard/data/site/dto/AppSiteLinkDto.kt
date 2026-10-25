package com.nativelap.heartguard.data.site.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppSiteDashboardDto(
    val summary: AppSiteDashboardSummaryDto,
)

@Serializable
data class AppSiteDashboardSummaryDto(
    val todayRecordCount: Int,
    val activeEmergencyCount: Int,
)

@Serializable
data class ClaimAppSiteLinkRequestDto(
    @SerialName("token")
    val token: String,
    @SerialName("managerPhone")
    val managerPhone: String,
) {
    override fun toString(): String = "ClaimAppSiteLinkRequestDto(token=***, managerPhone=***)"
}

@Serializable
data class AppSiteLinkClaimResponseDto(
    @SerialName("siteId")
    val siteId: String,
    @SerialName("siteName")
    val siteName: String,
    @SerialName("linkedAt")
    val linkedAt: String,
)

@Serializable
data class AppLinkedSitesResponseDto(
    @SerialName("items")
    val items: List<AppLinkedSiteDto> = emptyList(),
)

@Serializable
data class AppLinkedSiteDto(
    @SerialName("siteId")
    val siteId: String,
    @SerialName("siteName")
    val siteName: String,
    @SerialName("managerName")
    val managerName: String? = null,
    @SerialName("linkedAt")
    val linkedAt: String,
)

@Serializable
data class AppSiteSelectionResponseDto(
    @SerialName("siteId")
    val siteId: String,
    @SerialName("siteName")
    val siteName: String,
)
