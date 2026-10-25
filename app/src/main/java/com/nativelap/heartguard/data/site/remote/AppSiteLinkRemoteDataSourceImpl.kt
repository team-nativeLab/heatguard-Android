package com.nativelap.heartguard.data.site.remote

import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.RequestSessionGeneration
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
        private val appSiteDashboardApiService: AppSiteDashboardApiService,
        private val apiExecutor: ApiExecutor,
    ) : AppSiteLinkRemoteDataSource {
        override suspend fun getDashboard(
            siteId: String,
            expectedSessionGeneration: Long,
        ): ApiResult<AppSiteDashboardDto> =
            apiExecutor
                .execute {
                    appSiteDashboardApiService.getDashboard(
                        RequestSessionGeneration(expectedSessionGeneration),
                    )
                }.requireSuccessData { response ->
                    response.summary.todayRecordCount >= 0 && response.summary.activeEmergencyCount >= 0
                }

        override suspend fun unlinkSite(
            siteId: String,
            expectedSessionGeneration: Long,
        ): ApiResult<Unit> =
            apiExecutor.execute {
                appSiteLinkApiService.unlinkSite(
                    siteId,
                    RequestSessionGeneration(expectedSessionGeneration),
                )
            }

        override suspend fun claim(
            token: String,
            managerPhone: String,
            expectedSessionGeneration: Long,
        ): ApiResult<AppSiteLinkClaimResponseDto> =
            apiExecutor
                .execute {
                    appSiteLinkApiService.claim(
                        ClaimAppSiteLinkRequestDto(
                            token = token,
                            managerPhone = managerPhone,
                        ),
                        RequestSessionGeneration(expectedSessionGeneration),
                    )
                }.requireSuccessData { response ->
                    response.siteId.isNotBlank() && response.siteName.isNotBlank()
                }

        override suspend fun getLinkedSites(expectedSessionGeneration: Long): ApiResult<AppLinkedSitesResponseDto> =
            apiExecutor
                .execute {
                    appSiteLinkApiService.getLinkedSites(
                        RequestSessionGeneration(expectedSessionGeneration),
                    )
                }.requireSuccessData()

        override suspend fun selectSite(
            siteId: String,
            expectedSessionGeneration: Long,
        ): ApiResult<AppSiteSelectionResponseDto> =
            apiExecutor
                .execute {
                    appSiteLinkApiService.selectSite(
                        siteId,
                        RequestSessionGeneration(expectedSessionGeneration),
                    )
                }.requireSuccessData { response ->
                    response.siteId == siteId && response.siteName.isNotBlank()
                }
    }
