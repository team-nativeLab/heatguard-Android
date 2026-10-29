package com.nativelap.heartguard.data.site.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.data.site.dto.AppLinkedSiteDto
import com.nativelap.heartguard.data.site.dto.AppLinkedSitesResponseDto
import com.nativelap.heartguard.data.site.dto.AppSiteDashboardDto
import com.nativelap.heartguard.data.site.dto.AppSiteLinkClaimResponseDto
import com.nativelap.heartguard.data.site.dto.AppSiteSelectionResponseDto
import com.nativelap.heartguard.data.site.remote.AppSiteLinkRemoteDataSource
import com.nativelap.heartguard.domain.site.model.AppLinkedSite
import com.nativelap.heartguard.domain.site.model.AppLinkedSites
import com.nativelap.heartguard.domain.site.model.AppSelectedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class AppSiteLinkRepositoryImplTest {
    @Test
    fun `select team sends team id and stores response site id`() =
        runBlocking {
            val tokenStorage = FakeTokenStorage()
            val sessionManager = SessionManager(tokenStorage, Dispatchers.Unconfined)
            sessionManager.onLoginSucceeded(SessionToken("access-token"))
            val remoteDataSource = FakeAppSiteLinkRemoteDataSource()
            val repository = AppSiteLinkRepositoryImpl(remoteDataSource, sessionManager)

            val result = repository.selectTeam("team_01", sessionManager.getSnapshot().generation)

            assertEquals(
                ApiResult.Success(AppSelectedSite("team_01", "팀", "site_01")),
                result,
            )
            assertEquals("team_01", remoteDataSource.selectedTeamId)
            assertEquals("site_01", tokenStorage.selectedSiteId)
        }

    @Test
    fun `unlink sends team id and clears only matching site id`() =
        runBlocking {
            val tokenStorage = FakeTokenStorage(selectedSiteId = "site_01")
            val sessionManager = SessionManager(tokenStorage, Dispatchers.Unconfined)
            sessionManager.onLoginSucceeded(SessionToken("access-token"))
            val remoteDataSource = FakeAppSiteLinkRemoteDataSource()
            val repository = AppSiteLinkRepositoryImpl(remoteDataSource, sessionManager)
            val generation = sessionManager.getSnapshot().generation

            repository.unlinkTeam("team_02", "site_02", generation)
            assertEquals("team_02", remoteDataSource.unlinkedTeamId)
            assertEquals("site_01", tokenStorage.selectedSiteId)

            repository.unlinkTeam("team_01", "site_01", generation)
            assertEquals("team_01", remoteDataSource.unlinkedTeamId)
            assertEquals(null, tokenStorage.selectedSiteId)
        }

    @Test
    fun `get linked sites stores site id for selected team`() =
        runBlocking {
            val tokenStorage = FakeTokenStorage(selectedSiteId = "site_old")
            val sessionManager = SessionManager(tokenStorage, Dispatchers.Unconfined)
            sessionManager.onLoginSucceeded(SessionToken("access-token"))
            val remoteDataSource =
                FakeAppSiteLinkRemoteDataSource(
                    linkedSitesResponse =
                        AppLinkedSitesResponseDto(
                            items =
                                listOf(
                                    AppLinkedSiteDto(
                                        teamId = "team_01",
                                        teamName = "팀 1",
                                        siteId = "site_01",
                                        siteName = "현장 1",
                                        companyId = "company_01",
                                    ),
                                ),
                            selectedTeamId = "team_01",
                        ),
                )
            val repository = AppSiteLinkRepositoryImpl(remoteDataSource, sessionManager)

            val result = repository.getLinkedSites(sessionManager.getSnapshot().generation)

            assertEquals(
                ApiResult.Success(
                    AppLinkedSites(
                        items = listOf(AppLinkedSite("team_01", "팀 1", "site_01", "현장 1", "company_01")),
                        selectedTeamId = "team_01",
                    ),
                ),
                result,
            )
            assertEquals("site_01", tokenStorage.selectedSiteId)
        }

    @Test
    fun `get linked sites clears previous site when no team is selected`() =
        runBlocking {
            val tokenStorage = FakeTokenStorage(selectedSiteId = "site_old")
            val sessionManager = SessionManager(tokenStorage, Dispatchers.Unconfined)
            sessionManager.onLoginSucceeded(SessionToken("access-token"))
            val remoteDataSource =
                FakeAppSiteLinkRemoteDataSource(
                    linkedSitesResponse = AppLinkedSitesResponseDto(items = emptyList(), selectedTeamId = null),
                )
            val repository = AppSiteLinkRepositoryImpl(remoteDataSource, sessionManager)

            val result = repository.getLinkedSites(sessionManager.getSnapshot().generation)

            assertEquals(ApiResult.Success(AppLinkedSites(emptyList(), selectedTeamId = null)), result)
            assertEquals(null, tokenStorage.selectedSiteId)
        }
}

private class FakeTokenStorage(
    var selectedSiteId: String? = null,
) : TokenStorage {
    override fun readAccessToken(): String? = "access-token"

    override fun saveAccessToken(accessToken: String) = Unit

    override fun readSelectedSiteId(): String? = selectedSiteId

    override fun saveSelectedSiteId(siteId: String) {
        selectedSiteId = siteId
    }

    override fun clearSelectedSiteId() {
        selectedSiteId = null
    }

    override fun clear() = Unit
}

private class FakeAppSiteLinkRemoteDataSource(
    private val linkedSitesResponse: AppLinkedSitesResponseDto = AppLinkedSitesResponseDto(emptyList(), null),
) : AppSiteLinkRemoteDataSource {
    var selectedTeamId: String? = null
    var unlinkedTeamId: String? = null

    override suspend fun getDashboard(
        siteId: String,
        expectedSessionGeneration: Long,
    ): ApiResult<AppSiteDashboardDto> = ApiResult.Failure(com.nativelap.heartguard.core.network.ApiError.Unknown)

    override suspend fun unlinkTeam(
        teamId: String,
        expectedSessionGeneration: Long,
    ): ApiResult<Unit> {
        unlinkedTeamId = teamId
        return ApiResult.Success(Unit)
    }

    override suspend fun claim(
        token: String,
        managerPhone: String,
        expectedSessionGeneration: Long,
    ): ApiResult<AppSiteLinkClaimResponseDto> =
        ApiResult.Failure(com.nativelap.heartguard.core.network.ApiError.Unknown)

    override suspend fun getLinkedSites(expectedSessionGeneration: Long): ApiResult<AppLinkedSitesResponseDto> =
        ApiResult.Success(linkedSitesResponse)

    override suspend fun selectTeam(
        teamId: String,
        expectedSessionGeneration: Long,
    ): ApiResult<AppSiteSelectionResponseDto> {
        selectedTeamId = teamId
        return ApiResult.Success(
            AppSiteSelectionResponseDto(
                teamId = teamId,
                teamName = "팀",
                siteId = "site_01",
            ),
        )
    }
}
