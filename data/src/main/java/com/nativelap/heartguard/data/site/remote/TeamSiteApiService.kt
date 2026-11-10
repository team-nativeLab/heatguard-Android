package com.nativelap.heartguard.data.site.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.data.site.dto.TeamSiteResponseDto
import retrofit2.http.GET

interface TeamSiteApiService {
    @GET("api/v1/team")
    suspend fun getTeamSite(): ApiEnvelope<TeamSiteResponseDto>
}
