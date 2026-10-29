package com.nativelap.heartguard.data.site.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.core.network.RequestSessionGeneration
import com.nativelap.heartguard.data.site.dto.AppLinkedSitesResponseDto
import com.nativelap.heartguard.data.site.dto.AppSiteLinkClaimResponseDto
import com.nativelap.heartguard.data.site.dto.AppSiteSelectionResponseDto
import com.nativelap.heartguard.data.site.dto.ClaimAppSiteLinkRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Tag

interface AppSiteLinkApiService {
    @DELETE("api/v1/team/memberships/{teamId}")
    suspend fun unlinkTeam(
        @Path("teamId") teamId: String,
        @Tag owningSession: RequestSessionGeneration,
    )

    @POST("api/v1/team/invitations/accept")
    suspend fun claim(
        @Body request: ClaimAppSiteLinkRequestDto,
        @Tag owningSession: RequestSessionGeneration,
    ): ApiEnvelope<AppSiteLinkClaimResponseDto>

    @GET("api/v1/team/memberships")
    suspend fun getLinkedSites(
        @Tag owningSession: RequestSessionGeneration,
    ): ApiEnvelope<AppLinkedSitesResponseDto>

    @POST("api/v1/team/memberships/{teamId}/select")
    suspend fun selectTeam(
        @Path("teamId") teamId: String,
        @Tag owningSession: RequestSessionGeneration,
    ): ApiEnvelope<AppSiteSelectionResponseDto>
}
