package com.nativelap.heartguard.data.site.remote

import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.requireSuccessData
import com.nativelap.heartguard.data.site.dto.AppLinkedSitesResponseDto
import com.nativelap.heartguard.data.site.dto.AppSiteDashboardDto
import com.nativelap.heartguard.data.site.dto.AppSiteLinkClaimResponseDto
import com.nativelap.heartguard.data.site.dto.AppSiteSelectionResponseDto
import com.nativelap.heartguard.data.site.dto.ClaimAppSiteLinkRequestDto
import javax.inject.Inject

class AppSiteLinkRemoteDataSourceImpl
    @Inject
    constructor(
        private val appSiteLinkApiService: AppSiteLinkApiService,
        private val apiExecutor: ApiExecutor,
    ) : AppSiteLinkRemoteDataSource {
        override suspend fun getDashboard(siteId: String): ApiResult<AppSiteDashboardDto> =
            apiExecutor
                .execute { appSiteLinkApiService.getDashboard(siteId) }
                .requireSuccessData { response ->
                    response.summary.todayRecordCount >= 0 && response.summary.activeEmergencyCount >= 0
                }

        override suspend fun unlinkSite(siteId: String): ApiResult<Unit> =
            apiExecutor.execute { appSiteLinkApiService.unlinkSite(siteId) }

        override suspend fun claim(
            token: String,
            managerPhone: String,
        ): ApiResult<AppSiteLinkClaimResponseDto> =
            apiExecutor
                .execute {
                    appSiteLinkApiService.claim(
                        ClaimAppSiteLinkRequestDto(
                            token = token,
                            managerPhone = managerPhone,
                        ),
                    )
                }.requireSuccessData { response ->
                    response.siteId.isNotBlank() && response.siteName.isNotBlank()
                }

        override suspend fun getLinkedSites(): ApiResult<AppLinkedSitesResponseDto> =
            apiExecutor
                .execute { appSiteLinkApiService.getLinkedSites() }
                .requireSuccessData()

        override suspend fun selectSite(siteId: String): ApiResult<AppSiteSelectionResponseDto> =
            apiExecutor
                .execute { appSiteLinkApiService.selectSite(siteId) }
                .requireSuccessData { response ->
                    response.siteId == siteId && response.siteName.isNotBlank()
                }
    }
