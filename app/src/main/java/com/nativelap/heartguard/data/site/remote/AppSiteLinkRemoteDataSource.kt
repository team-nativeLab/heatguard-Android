package com.nativelap.heartguard.data.site.remote

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.site.dto.AppLinkedSitesResponseDto
import com.nativelap.heartguard.data.site.dto.AppSiteDashboardDto
import com.nativelap.heartguard.data.site.dto.AppSiteLinkClaimResponseDto
import com.nativelap.heartguard.data.site.dto.AppSiteSelectionResponseDto

interface AppSiteLinkRemoteDataSource {
    suspend fun getDashboard(
        siteId: String,
        expectedSessionGeneration: Long,
    ): ApiResult<AppSiteDashboardDto>

    suspend fun unlinkSite(
        siteId: String,
        expectedSessionGeneration: Long,
    ): ApiResult<Unit>

    suspend fun claim(
        token: String,
        managerPhone: String,
        expectedSessionGeneration: Long,
    ): ApiResult<AppSiteLinkClaimResponseDto>

    suspend fun getLinkedSites(expectedSessionGeneration: Long): ApiResult<AppLinkedSitesResponseDto>

    suspend fun selectSite(
        siteId: String,
        expectedSessionGeneration: Long,
    ): ApiResult<AppSiteSelectionResponseDto>
}
