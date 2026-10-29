package com.nativelap.heartguard.data.site.repository

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.map
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionRole
import com.nativelap.heartguard.data.site.mapper.toDomain
import com.nativelap.heartguard.data.site.remote.AppSiteLinkRemoteDataSource
import com.nativelap.heartguard.domain.site.model.AppLinkedSites
import com.nativelap.heartguard.domain.site.model.AppSelectedSite
import com.nativelap.heartguard.domain.site.model.AppSiteDashboard
import com.nativelap.heartguard.domain.site.model.AppSiteLinkClaim
import com.nativelap.heartguard.domain.site.repository.AppSiteLinkRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class AppSiteLinkRepositoryImpl
    @Inject
    constructor(
        private val appSiteLinkRemoteDataSource: AppSiteLinkRemoteDataSource,
        private val sessionManager: SessionManager,
    ) : AppSiteLinkRepository {
        override suspend fun getDashboard(
            siteId: String,
            expectedSessionGeneration: Long,
        ): ApiResult<AppSiteDashboard> {
            val requestSession = sessionManager.getSnapshot()
            if (siteId.isBlank() ||
                requestSession.role != SessionRole.TEAM_MEMBER ||
                requestSession.generation != expectedSessionGeneration ||
                sessionManager.getSelectedSiteId() != siteId
            ) {
                return ApiResult.Failure(ApiError.SiteNotSelected)
            }
            return appSiteLinkRemoteDataSource.getDashboard(siteId, expectedSessionGeneration).map { response ->
                AppSiteDashboard(
                    todayRecordCount = response.summary.todayRecordCount,
                    activeEmergencyCount = response.summary.activeEmergencyCount,
                )
            }
        }

        override suspend fun claim(
            token: String,
            managerPhone: String,
            expectedSessionGeneration: Long,
        ): ApiResult<AppSiteLinkClaim> =
            sessionManager.getSnapshot().let { requestSession ->
                if (requestSession.role != SessionRole.TEAM_MEMBER ||
                    requestSession.generation != expectedSessionGeneration
                ) {
                    return ApiResult.Failure(ApiError.SessionChanged)
                }
                appSiteLinkRemoteDataSource
                    .claim(
                        token = token,
                        managerPhone = managerPhone.filter(Char::isDigit),
                        expectedSessionGeneration = expectedSessionGeneration,
                    ).map { response -> response.toDomain() }
            }

        override suspend fun getLinkedSites(expectedSessionGeneration: Long): ApiResult<AppLinkedSites> {
            val requestSession = sessionManager.getSnapshot()
            if (requestSession.role != SessionRole.TEAM_MEMBER ||
                requestSession.generation != expectedSessionGeneration
            ) {
                return ApiResult.Failure(ApiError.SessionChanged)
            }
            val selectedSiteIdAtRequest = sessionManager.getSelectedSiteId()
            val result = appSiteLinkRemoteDataSource.getLinkedSites(expectedSessionGeneration)
            if (result is ApiResult.Failure) {
                return result
            }
            result as ApiResult.Success
            return try {
                val linkedSites = result.value.toDomain()
                val selectedTeam =
                    linkedSites.items.firstOrNull { site ->
                        site.teamId == linkedSites.selectedTeamId
                    }
                val selectedSiteId = selectedTeam?.siteId
                if (selectedSiteIdAtRequest != selectedSiteId) {
                    if (selectedSiteId == null) {
                        selectedSiteIdAtRequest?.let { previousSiteId ->
                            sessionManager.clearSelectedSiteIdForSession(requestSession, previousSiteId)
                        }
                    } else {
                        sessionManager.saveSelectedSiteIdForSession(selectedSiteId, requestSession)
                    }
                }
                ApiResult.Success(
                    linkedSites.copy(selectedTeamId = selectedTeam?.teamId),
                )
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                ApiResult.Failure(ApiError.LocalStorage)
            }
        }

        override suspend fun unlinkTeam(
            teamId: String,
            siteId: String,
            expectedSessionGeneration: Long,
        ): ApiResult<Unit> {
            if (teamId.isBlank() || siteId.isBlank()) {
                return ApiResult.Failure(ApiError.Unknown)
            }
            val requestSession = sessionManager.getSnapshot()
            if (requestSession.role != SessionRole.TEAM_MEMBER ||
                requestSession.generation != expectedSessionGeneration
            ) {
                return ApiResult.Failure(ApiError.SessionChanged)
            }
            val selectedSiteIdAtRequest = sessionManager.getSelectedSiteId()
            val result = appSiteLinkRemoteDataSource.unlinkTeam(teamId, expectedSessionGeneration)
            if (result is ApiResult.Success) {
                try {
                    if (selectedSiteIdAtRequest == siteId) {
                        sessionManager.clearSelectedSiteIdForSession(requestSession, siteId)
                    }
                } catch (cancellationException: CancellationException) {
                    throw cancellationException
                } catch (_: Exception) {
                    return ApiResult.Failure(ApiError.LocalStorage)
                }
            }
            return result
        }

        override suspend fun selectTeam(
            teamId: String,
            expectedSessionGeneration: Long,
        ): ApiResult<AppSelectedSite> {
            if (teamId.isBlank()) {
                return ApiResult.Failure(ApiError.Unknown)
            }
            val requestSession = sessionManager.getSnapshot()
            if (requestSession.role != SessionRole.TEAM_MEMBER) {
                return ApiResult.Failure(ApiError.SessionChanged)
            }
            if (requestSession.generation != expectedSessionGeneration) {
                return ApiResult.Failure(ApiError.SessionChanged)
            }
            return when (
                val result =
                    appSiteLinkRemoteDataSource.selectTeam(
                        teamId,
                        expectedSessionGeneration,
                    )
            ) {
                is ApiResult.Failure -> {
                    result
                }

                is ApiResult.Success -> {
                    try {
                        if (sessionManager.saveSelectedSiteIdForSession(result.value.siteId, requestSession)) {
                            ApiResult.Success(result.value.toDomain())
                        } else {
                            ApiResult.Failure(ApiError.SessionChanged)
                        }
                    } catch (cancellationException: CancellationException) {
                        throw cancellationException
                    } catch (_: Exception) {
                        ApiResult.Failure(ApiError.LocalStorage)
                    }
                }
            }
        }

        override fun getSelectedSiteId(): String? = sessionManager.getSelectedSiteId()
    }
