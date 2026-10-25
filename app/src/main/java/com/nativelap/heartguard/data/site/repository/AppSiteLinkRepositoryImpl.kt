package com.nativelap.heartguard.data.site.repository

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.map
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionRole
import com.nativelap.heartguard.data.site.mapper.toDomain
import com.nativelap.heartguard.data.site.remote.AppSiteLinkRemoteDataSource
import com.nativelap.heartguard.domain.site.model.AppLinkedSite
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
                requestSession.role != SessionRole.APP_ACCOUNT ||
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
                if (requestSession.role != SessionRole.APP_ACCOUNT ||
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

        override suspend fun getLinkedSites(expectedSessionGeneration: Long): ApiResult<List<AppLinkedSite>> {
            val requestSession = sessionManager.getSnapshot()
            if (requestSession.role != SessionRole.APP_ACCOUNT ||
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
                val sites = result.value.items.map { site -> site.toDomain() }
                if (selectedSiteIdAtRequest != null && sites.none { it.siteId == selectedSiteIdAtRequest }) {
                    sessionManager.clearSelectedSiteIdForSession(requestSession, selectedSiteIdAtRequest)
                }
                ApiResult.Success(sites)
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                ApiResult.Failure(ApiError.LocalStorage)
            }
        }

        override suspend fun unlinkSite(
            siteId: String,
            expectedSessionGeneration: Long,
        ): ApiResult<Unit> {
            if (siteId.isBlank()) {
                return ApiResult.Failure(ApiError.Unknown)
            }
            val requestSession = sessionManager.getSnapshot()
            if (requestSession.role != SessionRole.APP_ACCOUNT ||
                requestSession.generation != expectedSessionGeneration
            ) {
                return ApiResult.Failure(ApiError.SessionChanged)
            }
            val selectedSiteIdAtRequest = sessionManager.getSelectedSiteId()
            val result = appSiteLinkRemoteDataSource.unlinkSite(siteId, expectedSessionGeneration)
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

        override suspend fun selectSite(
            siteId: String,
            expectedSessionGeneration: Long,
        ): ApiResult<AppSelectedSite> {
            if (siteId.isBlank()) {
                return ApiResult.Failure(ApiError.Unknown)
            }
            val requestSession = sessionManager.getSnapshot()
            if (requestSession.role != SessionRole.APP_ACCOUNT) {
                return ApiResult.Failure(ApiError.SessionChanged)
            }
            if (requestSession.generation != expectedSessionGeneration) {
                return ApiResult.Failure(ApiError.SessionChanged)
            }
            return when (val result = appSiteLinkRemoteDataSource.selectSite(siteId, expectedSessionGeneration)) {
                is ApiResult.Failure -> {
                    result
                }

                is ApiResult.Success -> {
                    try {
                        if (sessionManager.saveSelectedSiteIdForSession(siteId, requestSession)) {
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
