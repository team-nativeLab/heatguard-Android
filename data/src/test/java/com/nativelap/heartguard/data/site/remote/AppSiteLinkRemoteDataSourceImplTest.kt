package com.nativelap.heartguard.data.site.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.RequestSessionGeneration
import com.nativelap.heartguard.data.site.dto.AppLinkedSiteDto
import com.nativelap.heartguard.data.site.dto.AppLinkedSitesResponseDto
import com.nativelap.heartguard.data.site.dto.AppSiteDashboardDto
import com.nativelap.heartguard.data.site.dto.AppSiteDashboardSummaryDto
import com.nativelap.heartguard.data.site.dto.AppSiteLinkClaimResponseDto
import com.nativelap.heartguard.data.site.dto.AppSiteSelectionResponseDto
import com.nativelap.heartguard.data.site.dto.ClaimAppSiteLinkRequestDto
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AppSiteLinkRemoteDataSourceImplTest {
    @Test
    fun `linked sites rejects blank identifiers`() =
        runBlocking {
            val invalidItems =
                listOf(
                    linkedSite(teamId = "", siteId = "site_01"),
                    linkedSite(teamId = "team_01", siteId = ""),
                )

            invalidItems.forEach { invalidItem ->
                val result =
                    remoteDataSource(
                        AppLinkedSitesResponseDto(
                            items = listOf(invalidItem),
                            selectedTeamId = null,
                        ),
                    ).getLinkedSites(expectedSessionGeneration = 1L)

                assertEquals(ApiResult.Failure(ApiError.Serialization), result)
            }
        }

    @Test
    fun `linked sites rejects duplicate team identifiers`() =
        runBlocking {
            val response =
                AppLinkedSitesResponseDto(
                    items =
                        listOf(
                            linkedSite(teamId = "team_01", siteId = "site_01"),
                            linkedSite(teamId = "team_01", siteId = "site_02"),
                        ),
                    selectedTeamId = "team_01",
                )

            val result = remoteDataSource(response).getLinkedSites(expectedSessionGeneration = 1L)

            assertEquals(ApiResult.Failure(ApiError.Serialization), result)
        }

    @Test
    fun `linked sites rejects selected team missing from items`() =
        runBlocking {
            val response =
                AppLinkedSitesResponseDto(
                    items = listOf(linkedSite(teamId = "team_01", siteId = "site_01")),
                    selectedTeamId = "team_missing",
                )

            val result = remoteDataSource(response).getLinkedSites(expectedSessionGeneration = 1L)

            assertEquals(ApiResult.Failure(ApiError.Serialization), result)
        }

    @Test
    fun `select and unlink pass team id to api service`() =
        runBlocking {
            val apiService = FakeAppSiteLinkApiService()
            val dataSource = remoteDataSource(apiService = apiService)

            val selectionResult = dataSource.selectTeam("team_01", expectedSessionGeneration = 1L)
            val unlinkResult = dataSource.unlinkTeam("team_02", expectedSessionGeneration = 1L)

            assertIs<ApiResult.Success<AppSiteSelectionResponseDto>>(selectionResult)
            assertEquals("team_01", apiService.selectedTeamId)
            assertIs<ApiResult.Success<Unit>>(unlinkResult)
            assertEquals("team_02", apiService.unlinkedTeamId)
        }

    @Test
    fun `select rejects response for a different team id`() =
        runBlocking {
            val apiService =
                FakeAppSiteLinkApiService(
                    selectionResponse = AppSiteSelectionResponseDto("team_02", "팀 2", "site_02"),
                )
            val result =
                remoteDataSource(apiService = apiService)
                    .selectTeam("team_01", expectedSessionGeneration = 1L)

            assertEquals(ApiResult.Failure(ApiError.Serialization), result)
            assertEquals("team_01", apiService.selectedTeamId)
        }

    private fun remoteDataSource(
        response: AppLinkedSitesResponseDto = AppLinkedSitesResponseDto(emptyList(), null),
        apiService: FakeAppSiteLinkApiService = FakeAppSiteLinkApiService(linkedSitesResponse = response),
    ): AppSiteLinkRemoteDataSourceImpl =
        AppSiteLinkRemoteDataSourceImpl(
            appSiteLinkApiService = apiService,
            appSiteDashboardApiService = FakeAppSiteDashboardApiService(),
            apiExecutor = ApiExecutor(),
        )

    private fun linkedSite(
        teamId: String,
        siteId: String,
    ): AppLinkedSiteDto =
        AppLinkedSiteDto(
            teamId = teamId,
            teamName = "팀",
            siteId = siteId,
            siteName = "현장",
            companyId = "company_01",
        )
}

private class FakeAppSiteLinkApiService(
    private val linkedSitesResponse: AppLinkedSitesResponseDto = AppLinkedSitesResponseDto(emptyList(), null),
    private val selectionResponse: AppSiteSelectionResponseDto? = null,
) : AppSiteLinkApiService {
    var selectedTeamId: String? = null
    var unlinkedTeamId: String? = null

    override suspend fun unlinkTeam(
        teamId: String,
        owningSession: RequestSessionGeneration,
    ) {
        unlinkedTeamId = teamId
    }

    override suspend fun claim(
        request: ClaimAppSiteLinkRequestDto,
        owningSession: RequestSessionGeneration,
    ): ApiEnvelope<AppSiteLinkClaimResponseDto> =
        ApiEnvelope(
            success = true,
            data = AppSiteLinkClaimResponseDto("site_01", "현장", "2026-10-29T00:00:00Z"),
        )

    override suspend fun getLinkedSites(
        owningSession: RequestSessionGeneration,
    ): ApiEnvelope<AppLinkedSitesResponseDto> = ApiEnvelope(success = true, data = linkedSitesResponse)

    override suspend fun selectTeam(
        teamId: String,
        owningSession: RequestSessionGeneration,
    ): ApiEnvelope<AppSiteSelectionResponseDto> {
        selectedTeamId = teamId
        return ApiEnvelope(
            success = true,
            data = selectionResponse ?: AppSiteSelectionResponseDto(teamId, "팀", "site_01"),
        )
    }
}

private class FakeAppSiteDashboardApiService : AppSiteDashboardApiService {
    override suspend fun getDashboard(owningSession: RequestSessionGeneration): ApiEnvelope<AppSiteDashboardDto> =
        ApiEnvelope(
            success = true,
            data = AppSiteDashboardDto(AppSiteDashboardSummaryDto(0, 0)),
        )
}
