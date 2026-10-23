package com.nativelap.heartguard.data.site.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.data.site.dto.AppLinkedSitesResponseDto
import com.nativelap.heartguard.data.site.dto.AppSiteLinkClaimResponseDto
import com.nativelap.heartguard.data.site.dto.AppSiteSelectionResponseDto
import com.nativelap.heartguard.data.site.dto.ClaimAppSiteLinkRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AppSiteLinkApiService {
    @POST("api/v1/app/links/claim")
    suspend fun claim(
        @Body request: ClaimAppSiteLinkRequestDto,
    ): ApiEnvelope<AppSiteLinkClaimResponseDto>

    @GET("api/v1/app/sites")
    suspend fun getLinkedSites(): ApiEnvelope<AppLinkedSitesResponseDto>

    @POST("api/v1/app/sites/{siteId}/select")
    suspend fun selectSite(
        @Path("siteId") siteId: String,
    ): ApiEnvelope<AppSiteSelectionResponseDto>
}
