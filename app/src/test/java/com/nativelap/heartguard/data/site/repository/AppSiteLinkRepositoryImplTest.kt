package com.nativelap.heartguard.data.site.repository

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.data.site.dto.AppLinkedSiteDto
import com.nativelap.heartguard.data.site.dto.AppLinkedSitesResponseDto
import com.nativelap.heartguard.data.site.dto.AppSiteDashboardDto
import com.nativelap.heartguard.data.site.dto.AppSiteLinkClaimResponseDto
import com.nativelap.heartguard.data.site.dto.AppSiteSelectionResponseDto
import com.nativelap.heartguard.data.site.remote.AppSiteLinkRemoteDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppSiteLinkRepositoryImplTest {
    @Test
    fun selectedSiteUnlinkClearsSelectionAndOtherSiteUnlinkPreservesIt() =
        runTest {
            val storage = SiteSelectionStorage("a")
            val repository = AppSiteLinkRepositoryImpl(SiteLinkRemote(), storage)

            repository.unlinkSite("b")
            assertEquals("a", storage.selected)
            repository.unlinkSite("a")
            assertNull(storage.selected)
        }

    @Test
    fun failedUnlinkPreservesSelectedSite() =
        runTest {
            val storage = SiteSelectionStorage("a")
            val remote = SiteLinkRemote(unlinkResult = ApiResult.Failure(ApiError.Network))

            AppSiteLinkRepositoryImpl(remote, storage).unlinkSite("a")

            assertEquals("a", storage.selected)
        }

    @Test
    fun webUnlinkClearsOnlyMissingSelectedSiteAfterListRefresh() =
        runTest {
            val storage = SiteSelectionStorage("a")
            val remote = SiteLinkRemote()
            val repository = AppSiteLinkRepositoryImpl(remote, storage)

            repository.getLinkedSites()
            assertEquals("a", storage.selected)
            remote.sites = emptyList()
            repository.getLinkedSites()
            assertNull(storage.selected)
        }
}

private class SiteSelectionStorage(
    var selected: String?,
) : TokenStorage {
    override fun readSelectedSiteId(): String? = selected

    override fun clearSelectedSiteId() {
        selected = null
    }

    override fun readAccessToken(): String? = null

    override fun saveAccessToken(accessToken: String) = Unit

    override fun clear() = Unit
}

private class SiteLinkRemote(
    val unlinkResult: ApiResult<Unit> = ApiResult.Success(Unit),
) : AppSiteLinkRemoteDataSource {
    var sites = listOf(AppLinkedSiteDto("a", "현장 A", null, "2026-10-25"))

    override suspend fun unlinkSite(siteId: String): ApiResult<Unit> = unlinkResult

    override suspend fun getDashboard(siteId: String): ApiResult<AppSiteDashboardDto> = error("Not used")

    override suspend fun getLinkedSites(): ApiResult<AppLinkedSitesResponseDto> =
        ApiResult.Success(AppLinkedSitesResponseDto(sites))

    override suspend fun claim(
        token: String,
        managerPhone: String,
    ): ApiResult<AppSiteLinkClaimResponseDto> = error("Not used")

    override suspend fun selectSite(siteId: String): ApiResult<AppSiteSelectionResponseDto> = error("Not used")
}
