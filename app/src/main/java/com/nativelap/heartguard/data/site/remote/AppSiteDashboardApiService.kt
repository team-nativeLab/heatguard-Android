package com.nativelap.heartguard.data.site.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.data.site.dto.AppSiteDashboardDto
import retrofit2.http.GET

/** 선택된 현장 ID를 인증 모드가 자동으로 헤더에 넣는 현장 범위 API다. */
interface AppSiteDashboardApiService {
    @GET("api/v1/site/dashboard")
    suspend fun getDashboard(): ApiEnvelope<AppSiteDashboardDto>
}
