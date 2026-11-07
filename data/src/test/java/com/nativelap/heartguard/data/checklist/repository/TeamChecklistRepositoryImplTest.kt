package com.nativelap.heartguard.data.checklist.repository

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionRole
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.data.checklist.dto.ChecklistItemStatusDto
import com.nativelap.heartguard.data.checklist.dto.ChecklistRoundItemStatusDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistResponseDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistRoundDetailResponseDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistRoundsResponseDto
import com.nativelap.heartguard.data.checklist.dto.UpdateChecklistItemRequestDto
import com.nativelap.heartguard.data.checklist.dto.UpdateChecklistRoundItemRequestDto
import com.nativelap.heartguard.data.checklist.remote.TeamChecklistRemoteDataSource
import com.nativelap.heartguard.domain.site.model.AppLinkedSites
import com.nativelap.heartguard.domain.site.repository.AppSiteLinkRepository
import com.nativelap.heartguard.domain.site.model.AppSiteDashboard
import com.nativelap.heartguard.domain.site.model.AppSiteLinkClaim
import com.nativelap.heartguard.domain.site.model.AppSelectedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class TeamChecklistRepositoryImplTest {
    @Test
    fun `does not call checklist remote before membership retry confirms selected team`() = runBlocking {
        val tokenStorage = TestTokenStorage()
        val sessionManager = SessionManager(tokenStorage, Dispatchers.Unconfined)
        sessionManager.onLoginSucceeded(SessionToken("token"))
        var membershipConfirmed = false
        var checklistCalls = 0
        val siteRepository = object : AppSiteLinkRepository {
            override suspend fun getDashboard(siteId: String, expectedSessionGeneration: Long): ApiResult<AppSiteDashboard> = ApiResult.Failure(ApiError.Unknown)
            override suspend fun unlinkTeam(teamId: String, siteId: String, expectedSessionGeneration: Long): ApiResult<Unit> = ApiResult.Failure(ApiError.Unknown)
            override suspend fun claim(token: String, managerPhone: String, expectedSessionGeneration: Long): ApiResult<AppSiteLinkClaim> = ApiResult.Failure(ApiError.Unknown)
            override suspend fun getLinkedSites(expectedSessionGeneration: Long): ApiResult<AppLinkedSites> {
                if (!membershipConfirmed) return ApiResult.Failure(ApiError.Network)
                sessionManager.saveSelectedTeamAndSiteIdsForSession("team", "site", null, null, sessionManager.getSnapshot())
                return ApiResult.Success(AppLinkedSites(emptyList(), "team"))
            }
            override suspend fun selectTeam(teamId: String, expectedSessionGeneration: Long): ApiResult<AppSelectedSite> = ApiResult.Failure(ApiError.Unknown)
            override fun getSelectedSiteId(): String? = null
        }
        val remoteDataSource = object : TeamChecklistRemoteDataSource {
            override suspend fun getChecklist(): ApiResult<TeamChecklistResponseDto> {
                checklistCalls += 1
                return ApiResult.Success(TeamChecklistResponseDto(date = "2026-11-07"))
            }
            override suspend fun setItemChecked(itemId: String, request: UpdateChecklistItemRequestDto): ApiResult<ChecklistItemStatusDto> = ApiResult.Failure(ApiError.Unknown)
            override suspend fun getRounds(date: String): ApiResult<TeamChecklistRoundsResponseDto> = ApiResult.Failure(ApiError.Unknown)
            override suspend fun getRound(roundId: String, date: String): ApiResult<TeamChecklistRoundDetailResponseDto> = ApiResult.Failure(ApiError.Unknown)
            override suspend fun setRoundItemChecked(roundId: String, itemId: String, request: UpdateChecklistRoundItemRequestDto): ApiResult<ChecklistRoundItemStatusDto> = ApiResult.Failure(ApiError.Unknown)
        }
        val repository = TeamChecklistRepositoryImpl(remoteDataSource, siteRepository, sessionManager)

        assertEquals(ApiResult.Failure(ApiError.TeamNotSelected), repository.getChecklist())
        assertEquals(0, checklistCalls)
        membershipConfirmed = true
        assertEquals(true, repository.getChecklist() is ApiResult.Success)
        assertEquals(1, checklistCalls)
    }

    @Test
    fun `getChecklist maps malformed server date to unknown failure`() =
        runBlocking {
            val remoteDataSource =
                object : TeamChecklistRemoteDataSource {
                    override suspend fun getChecklist(): ApiResult<TeamChecklistResponseDto> =
                        ApiResult.Success(TeamChecklistResponseDto(date = "not-a-date"))

                    override suspend fun setItemChecked(
                        itemId: String,
                        request: UpdateChecklistItemRequestDto,
                    ): ApiResult<ChecklistItemStatusDto> = ApiResult.Failure(ApiError.Unknown)

                    override suspend fun getRounds(date: String): ApiResult<TeamChecklistRoundsResponseDto> =
                        ApiResult.Failure(ApiError.Unknown)

                    override suspend fun getRound(
                        roundId: String,
                        date: String,
                    ): ApiResult<TeamChecklistRoundDetailResponseDto> = ApiResult.Failure(ApiError.Unknown)

                    override suspend fun setRoundItemChecked(
                        roundId: String,
                        itemId: String,
                        request: UpdateChecklistRoundItemRequestDto,
                    ): ApiResult<ChecklistRoundItemStatusDto> = ApiResult.Failure(ApiError.Unknown)
                }

            val tokenStorage = TestTokenStorage()
            val sessionManager = SessionManager(tokenStorage, Dispatchers.Unconfined)
            sessionManager.onLoginSucceeded(SessionToken("token"))
            sessionManager.saveSelectedTeamAndSiteIdsForSession("team", "site", null, null, sessionManager.getSnapshot())
            val siteRepository = object : AppSiteLinkRepository {
                override suspend fun getDashboard(siteId: String, expectedSessionGeneration: Long): ApiResult<AppSiteDashboard> = ApiResult.Failure(ApiError.Unknown)
                override suspend fun unlinkTeam(teamId: String, siteId: String, expectedSessionGeneration: Long): ApiResult<Unit> = ApiResult.Failure(ApiError.Unknown)
                override suspend fun claim(token: String, managerPhone: String, expectedSessionGeneration: Long): ApiResult<AppSiteLinkClaim> = ApiResult.Failure(ApiError.Unknown)
                override suspend fun getLinkedSites(expectedSessionGeneration: Long): ApiResult<AppLinkedSites> = ApiResult.Failure(ApiError.Unknown)
                override suspend fun selectTeam(teamId: String, expectedSessionGeneration: Long): ApiResult<AppSelectedSite> = ApiResult.Failure(ApiError.Unknown)
                override fun getSelectedSiteId(): String? = "site"
            }

            val result = TeamChecklistRepositoryImpl(remoteDataSource, siteRepository, sessionManager).getChecklist()

            assertEquals(ApiResult.Failure(ApiError.Unknown), result)
        }
}

private class TestTokenStorage : TokenStorage {
    private var teamId: String? = null
    private var siteId: String? = null
    override fun readAccessToken(): String? = null
    override fun saveAccessToken(accessToken: String) = Unit
    override fun readSessionRole(): SessionRole = SessionRole.TEAM_MEMBER
    override fun saveSession(accessToken: String, role: SessionRole) { teamId = null; siteId = null }
    override fun readSelectedTeamId(): String? = teamId
    override fun readSelectedSiteId(): String? = siteId
    override fun saveSelectedTeamAndSiteIds(teamId: String, siteId: String) { this.teamId = teamId; this.siteId = siteId }
    override fun clearSelectedTeamAndSiteIds() { teamId = null; siteId = null }
    override fun clear() { teamId = null; siteId = null }
}
