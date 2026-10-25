package com.nativelap.heartguard.domain.site.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.site.model.AppLinkedSite
import com.nativelap.heartguard.domain.site.model.AppSelectedSite
import com.nativelap.heartguard.domain.site.model.AppSiteDashboard
import com.nativelap.heartguard.domain.site.model.AppSiteLinkClaim

interface AppSiteLinkRepository {
    suspend fun getDashboard(siteId: String): ApiResult<AppSiteDashboard>

    suspend fun unlinkSite(siteId: String): ApiResult<Unit>

    suspend fun claim(
        token: String,
        managerPhone: String,
    ): ApiResult<AppSiteLinkClaim>

    suspend fun getLinkedSites(): ApiResult<List<AppLinkedSite>>

    suspend fun selectSite(siteId: String): ApiResult<AppSelectedSite>

    fun getSelectedSiteId(): String?
}
