package com.nativelap.heartguard.data.site.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.core.network.RequestSessionGeneration
import com.nativelap.heartguard.data.site.dto.AppSiteDashboardDto
import retrofit2.http.GET
import retrofit2.http.Tag

/** 선택된 현장 ID를 인증 모드가 자동으로 헤더에 넣는 현장 범위 API다. */
interface AppSiteDashboardApiService {
    @GET("api/v1/site/dashboard")
    suspend fun getDashboard(
        @Tag owningSession: RequestSessionGeneration,
    ): ApiEnvelope<AppSiteDashboardDto>
}
