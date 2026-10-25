package com.nativelap.heartguard.data.site.repository

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.map
import com.nativelap.heartguard.core.session.TokenStorage
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
        private val tokenStorage: TokenStorage,
    ) : AppSiteLinkRepository {
        override suspend fun getDashboard(siteId: String): ApiResult<AppSiteDashboard> {
            if (siteId.isBlank()) {
                return ApiResult.Failure(ApiError.Unknown)
            }
            return appSiteLinkRemoteDataSource.getDashboard(siteId).map { response ->
                AppSiteDashboard(
                    todayRecordCount = response.summary.todayRecordCount,
                    activeEmergencyCount = response.summary.activeEmergencyCount,
                )
            }
        }

        override suspend fun claim(
            token: String,
            managerPhone: String,
        ): ApiResult<AppSiteLinkClaim> =
            appSiteLinkRemoteDataSource
                .claim(
                    token = token,
                    managerPhone = managerPhone.filter(Char::isDigit),
                ).map { response -> response.toDomain() }

        override suspend fun getLinkedSites(): ApiResult<List<AppLinkedSite>> {
            val result = appSiteLinkRemoteDataSource.getLinkedSites()
            if (result is ApiResult.Failure) {
                return result
            }
            result as ApiResult.Success
            return try {
                val sites = result.value.items.map { site -> site.toDomain() }
                if (sites.none { it.siteId == tokenStorage.readSelectedSiteId() }) {
                    tokenStorage.clearSelectedSiteId()
                }
                ApiResult.Success(sites)
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                ApiResult.Failure(ApiError.LocalStorage)
            }
        }

        override suspend fun unlinkSite(siteId: String): ApiResult<Unit> {
            if (siteId.isBlank()) {
                return ApiResult.Failure(ApiError.Unknown)
            }
            val result = appSiteLinkRemoteDataSource.unlinkSite(siteId)
            if (result is ApiResult.Success) {
                try {
                    if (tokenStorage.readSelectedSiteId() == siteId) {
                        tokenStorage.clearSelectedSiteId()
                    }
                } catch (cancellationException: CancellationException) {
                    throw cancellationException
                } catch (_: Exception) {
                    return ApiResult.Failure(ApiError.LocalStorage)
                }
            }
            return result
        }

        override suspend fun selectSite(siteId: String): ApiResult<AppSelectedSite> {
            if (siteId.isBlank()) {
                return ApiResult.Failure(ApiError.Unknown)
            }
            return when (val result = appSiteLinkRemoteDataSource.selectSite(siteId)) {
                is ApiResult.Failure -> {
                    result
                }

                is ApiResult.Success -> {
                    try {
                        tokenStorage.saveSelectedSiteId(siteId)
                        ApiResult.Success(result.value.toDomain())
                    } catch (cancellationException: CancellationException) {
                        throw cancellationException
                    } catch (_: Exception) {
                        ApiResult.Failure(ApiError.LocalStorage)
                    }
                }
            }
        }

        override fun getSelectedSiteId(): String? = tokenStorage.readSelectedSiteId()
    }
