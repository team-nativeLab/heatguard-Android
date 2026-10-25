package com.nativelap.heartguard.viewmodel.site

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.createTestSessionManager
import com.nativelap.heartguard.domain.auth.model.AppAccountRegistration
import com.nativelap.heartguard.domain.auth.model.AppEmailCode
import com.nativelap.heartguard.domain.auth.repository.AppAuthRepository
import com.nativelap.heartguard.domain.auth.usecase.LogoutAppAccountUseCase
import com.nativelap.heartguard.domain.site.model.AppLinkedSite
import com.nativelap.heartguard.domain.site.model.AppSelectedSite
import com.nativelap.heartguard.domain.site.model.AppSiteDashboard
import com.nativelap.heartguard.domain.site.model.AppSiteLinkClaim
import com.nativelap.heartguard.domain.site.repository.AppSiteLinkRepository
import com.nativelap.heartguard.domain.site.usecase.GetAppLinkedSitesUseCase
import com.nativelap.heartguard.domain.site.usecase.GetAppSiteDashboardUseCase
import com.nativelap.heartguard.domain.site.usecase.GetSelectedAppSiteIdUseCase
import com.nativelap.heartguard.domain.site.usecase.SelectAppSiteUseCase
import com.nativelap.heartguard.domain.site.usecase.UnlinkAppSiteUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppAccountSitesViewModelTest {
    @Test
    fun selectionLoadsCorrectSiteAndWebUnlinkRemovesOpenedData() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val repository = DashboardSitesRepository()
                val viewModel = createViewModel(repository)
                advanceUntilIdle()
                viewModel.selectSite("a")
                advanceUntilIdle()
                assertEquals("a", repository.dashboardSite)
                assertEquals(
                    2,
                    viewModel.uiState.value.dashboard
                        ?.todayRecordCount,
                )

                viewModel.closeDashboard()
                viewModel.selectSite("b")
                advanceUntilIdle()
                assertEquals("b", repository.dashboardSite)
                repository.sites = repository.sites.filterNot { it.siteId == "b" }
                viewModel.refresh()
                advanceUntilIdle()
                assertNull(viewModel.uiState.value.openedSiteId)
                assertNull(viewModel.uiState.value.dashboard)
                assertEquals(
                    listOf("a"),
                    viewModel.uiState.value.linkedSites
                        .map { it.siteId },
                )
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun deniedDashboardNeverKeepsPreviousData() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val repository = DashboardSitesRepository()
                val viewModel = createViewModel(repository)
                advanceUntilIdle()
                viewModel.selectSite("a")
                advanceUntilIdle()
                repository.dashboardResult = ApiResult.Failure(ApiError.Http(403))
                viewModel.refresh()
                advanceUntilIdle()
                assertNull(viewModel.uiState.value.dashboard)
                assertTrue(viewModel.uiState.value.hasDashboardFailure)
            } finally {
                Dispatchers.resetMain()
            }
        }

    private fun createViewModel(repository: DashboardSitesRepository): AppAccountSitesViewModel =
        AppAccountSitesViewModel(
            GetAppLinkedSitesUseCase(repository),
            GetSelectedAppSiteIdUseCase(repository),
            SelectAppSiteUseCase(repository),
            UnlinkAppSiteUseCase(repository),
            GetAppSiteDashboardUseCase(repository),
            LogoutAppAccountUseCase(UnusedAppAuthRepository()),
            createTestSessionManager(Dispatchers.Unconfined),
        )
}

private class DashboardSitesRepository : AppSiteLinkRepository {
    var selected: String? = null
    var dashboardSite: String? = null
    var sites = listOf(AppLinkedSite("a", "현장 A", null, ""), AppLinkedSite("b", "현장 B", null, ""))
    var dashboardResult: ApiResult<AppSiteDashboard> = ApiResult.Success(AppSiteDashboard(2, 0))

    override suspend fun getDashboard(siteId: String): ApiResult<AppSiteDashboard> {
        dashboardSite = siteId
        return dashboardResult
    }

    override suspend fun unlinkSite(siteId: String): ApiResult<Unit> {
        sites = sites.filterNot { it.siteId == siteId }
        if (selected == siteId) {
            selected = null
        }
        return ApiResult.Success(Unit)
    }

    override suspend fun getLinkedSites(): ApiResult<List<AppLinkedSite>> = ApiResult.Success(sites)

    override suspend fun selectSite(siteId: String): ApiResult<AppSelectedSite> {
        selected = siteId
        return ApiResult.Success(AppSelectedSite(siteId, "현장"))
    }

    override fun getSelectedSiteId(): String? = selected

    override suspend fun claim(
        token: String,
        managerPhone: String,
    ): ApiResult<AppSiteLinkClaim> = error("Not used")
}

private class UnusedAppAuthRepository : AppAuthRepository {
    override suspend fun requestEmailCode(email: String): ApiResult<AppEmailCode> = error("Not used")

    override suspend fun register(
        email: String,
        password: String,
        confirmPassword: String,
        verificationCode: String,
    ): ApiResult<AppAccountRegistration> = error("Not used")

    override suspend fun login(
        email: String,
        password: String,
    ): ApiResult<Unit> = error("Not used")

    override suspend fun logout(): ApiResult<Unit> = ApiResult.Success(Unit)
}
