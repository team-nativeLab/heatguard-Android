package com.nativelap.heartguard.viewmodel.site

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionRole
import com.nativelap.heartguard.core.session.SessionToken
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
                val sessionManager = createAppAccountSession()
                val viewModel = createViewModel(repository, sessionManager)
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
                val sessionManager = createAppAccountSession()
                val viewModel = createViewModel(repository, sessionManager)
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

    @Test
    fun switchingAppAccountClearsOldSitesAndRejectsOldSessionResponse() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val sessionManager = createAppAccountSession()
                val oldAccountSites =
                    listOf(
                        AppLinkedSite("old-site", "계정 A 현장", "이전 관리자", "01011112222"),
                    )
                val newAccountSites =
                    listOf(
                        AppLinkedSite("new-site", "계정 B 현장", "새 관리자", "01033334444"),
                    )
                val repository =
                    DashboardSitesRepository().apply {
                        sites = oldAccountSites
                        selected = "old-site"
                    }
                val viewModel = createViewModel(repository, sessionManager)
                advanceUntilIdle()

                assertEquals(oldAccountSites, viewModel.uiState.value.linkedSites)
                assertEquals("old-site", viewModel.uiState.value.selectedSiteId)

                var accountBLoginStarted = false
                val statesAfterAccountBLogin = mutableListOf<AppAccountSitesUiState>()
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect { state ->
                        if (accountBLoginStarted) {
                            statesAfterAccountBLogin += state
                        }
                    }
                }

                repository.nextLinkedSitesResponse = {
                    accountBLoginStarted = true
                    repository.sites = newAccountSites
                    repository.selected = "new-site"
                    sessionManager.onLoginSucceeded(
                        SessionToken("account-b", SessionRole.APP_ACCOUNT),
                    )
                    ApiResult.Success(oldAccountSites)
                }
                viewModel.refresh()
                advanceUntilIdle()

                assertTrue(viewModel.uiState.value.linkedSites.none { it.siteId == "old-site" })
                assertEquals(newAccountSites, viewModel.uiState.value.linkedSites)
                assertEquals("new-site", viewModel.uiState.value.selectedSiteId)
                assertNull(viewModel.uiState.value.dashboard)
                assertTrue(statesAfterAccountBLogin.isNotEmpty())
                assertTrue(
                    statesAfterAccountBLogin.none { state ->
                        state.linkedSites.any { site -> site.siteId == "old-site" } ||
                            state.selectedSiteId == "old-site"
                    },
                )

            } finally {
                Dispatchers.resetMain()
            }
        }

    private suspend fun createAppAccountSession(): SessionManager =
        createTestSessionManager(Dispatchers.Unconfined).apply {
            onLoginSucceeded(SessionToken("account-a", SessionRole.APP_ACCOUNT))
        }

    private fun createViewModel(
        repository: DashboardSitesRepository,
        sessionManager: SessionManager,
    ): AppAccountSitesViewModel =
        AppAccountSitesViewModel(
            GetAppLinkedSitesUseCase(repository),
            GetSelectedAppSiteIdUseCase(repository),
            SelectAppSiteUseCase(repository),
            UnlinkAppSiteUseCase(repository),
            GetAppSiteDashboardUseCase(repository),
            LogoutAppAccountUseCase(UnusedAppAuthRepository()),
            sessionManager,
        )
}

private class DashboardSitesRepository : AppSiteLinkRepository {
    var selected: String? = null
    var dashboardSite: String? = null
    var sites = listOf(AppLinkedSite("a", "현장 A", null, ""), AppLinkedSite("b", "현장 B", null, ""))
    var dashboardResult: ApiResult<AppSiteDashboard> = ApiResult.Success(AppSiteDashboard(2, 0))
    var nextLinkedSitesResponse: (suspend () -> ApiResult<List<AppLinkedSite>>)? = null

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

    override suspend fun getLinkedSites(): ApiResult<List<AppLinkedSite>> {
        val pendingResponse = nextLinkedSitesResponse
        nextLinkedSitesResponse = null
        if (pendingResponse != null) {
            return pendingResponse()
        }
        return ApiResult.Success(sites)
    }

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
