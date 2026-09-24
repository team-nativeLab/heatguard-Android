package com.nativelap.heartguard.data.site.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.data.site.dto.TeamSiteResponseDto
import retrofit2.http.GET
import retrofit2.http.Path

interface TeamSiteApiService {
    @GET("api/v1/t/{teamToken}")
    suspend fun getTeamSite(
        @Path("teamToken") teamToken: String,
    ): ApiEnvelope<TeamSiteResponseDto>
}
