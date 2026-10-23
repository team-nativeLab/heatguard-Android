package com.nativelap.heartguard.data.site.remote

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.site.dto.AppLinkedSitesResponseDto
import com.nativelap.heartguard.data.site.dto.AppSiteLinkClaimResponseDto
import com.nativelap.heartguard.data.site.dto.AppSiteSelectionResponseDto

interface AppSiteLinkRemoteDataSource {
    suspend fun claim(
        token: String,
        managerPhone: String,
    ): ApiResult<AppSiteLinkClaimResponseDto>

    suspend fun getLinkedSites(): ApiResult<AppLinkedSitesResponseDto>

    suspend fun selectSite(siteId: String): ApiResult<AppSiteSelectionResponseDto>
}
