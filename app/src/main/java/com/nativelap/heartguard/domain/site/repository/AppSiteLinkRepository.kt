package com.nativelap.heartguard.domain.site.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.site.model.AppLinkedSite
import com.nativelap.heartguard.domain.site.model.AppSelectedSite
import com.nativelap.heartguard.domain.site.model.AppSiteDashboard
import com.nativelap.heartguard.domain.site.model.AppSiteLinkClaim

interface AppSiteLinkRepository {
    suspend fun getDashboard(
        siteId: String,
        expectedSessionGeneration: Long,
    ): ApiResult<AppSiteDashboard>

    suspend fun unlinkSite(
        siteId: String,
        expectedSessionGeneration: Long,
    ): ApiResult<Unit>

    suspend fun claim(
        token: String,
        managerPhone: String,
        expectedSessionGeneration: Long,
    ): ApiResult<AppSiteLinkClaim>

    suspend fun getLinkedSites(expectedSessionGeneration: Long): ApiResult<List<AppLinkedSite>>

    suspend fun selectSite(
        siteId: String,
        expectedSessionGeneration: Long,
    ): ApiResult<AppSelectedSite>

    fun getSelectedSiteId(): String?
}
