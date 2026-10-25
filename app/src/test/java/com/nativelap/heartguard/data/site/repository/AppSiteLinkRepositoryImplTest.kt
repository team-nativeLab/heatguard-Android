package com.nativelap.heartguard.data.site.repository

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.InMemoryTokenStorage
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionRole
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.data.site.dto.AppLinkedSitesResponseDto
import com.nativelap.heartguard.data.site.dto.AppLinkedSiteDto
import com.nativelap.heartguard.data.site.dto.AppSiteDashboardDto
import com.nativelap.heartguard.data.site.dto.AppSiteLinkClaimResponseDto
import com.nativelap.heartguard.data.site.dto.AppSiteSelectionResponseDto
import com.nativelap.heartguard.data.site.remote.AppSiteLinkRemoteDataSource
import com.nativelap.heartguard.domain.site.model.AppSelectedSite
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSiteLinkRepositoryImplTest {
    @Test
    fun `서버가 검증한 선택 성공 응답만 선택 ID로 저장한다`() = runBlocking {
        val storage = InMemoryTokenStorage()
        val sessionManager = createAppAccountSession(storage)
        val repository = createRepository(sessionManager) {
            ApiResult.Success(AppSiteSelectionResponseDto("site_01", "서울 현장"))
        }

        assertEquals(
            ApiResult.Success(AppSelectedSite("site_01", "서울 현장")),
            repository.selectSite("site_01"),
        )
        assertEquals("site_01", storage.selectedSiteId)
    }

    @Test
    fun `실패 응답이면 기존 선택 ID를 유지한다`() = runBlocking {
        val storage = InMemoryTokenStorage()
        val sessionManager = createAppAccountSession(storage)
        storage.saveSelectedSiteId("previous_site")
        val repository = createRepository(sessionManager) {
            ApiResult.Failure(ApiError.Http(statusCode = 500))
        }

        assertEquals(ApiResult.Failure(ApiError.Http(statusCode = 500)), repository.selectSite("site_01"))
        assertEquals("previous_site", storage.selectedSiteId)
    }

    @Test
    fun `네트워크 실패면 기존 선택 ID를 유지한다`() = runBlocking {
        val storage = InMemoryTokenStorage()
        val sessionManager = createAppAccountSession(storage)
        storage.saveSelectedSiteId("previous_site")
        val repository = createRepository(sessionManager) {
            ApiResult.Failure(ApiError.Network)
        }

        assertEquals(ApiResult.Failure(ApiError.Network), repository.selectSite("site_01"))
        assertEquals("previous_site", storage.selectedSiteId)
    }

    @Test
    fun `이전 계정의 늦은 선택 응답은 현재 계정 선택을 덮지 않는다`() = runBlocking {
        val storage = InMemoryTokenStorage()
        val sessionManager = createAppAccountSession(storage)
        val pendingSelection = CompletableDeferred<ApiResult<AppSiteSelectionResponseDto>>()
        val repository = createRepository(sessionManager) { pendingSelection.await() }

        val request = async { repository.selectSite("account_a_site") }
        yield()
        sessionManager.onLoginSucceeded(SessionToken("account-b-token", SessionRole.APP_ACCOUNT))
        storage.saveSelectedSiteId("account_b_site")
        pendingSelection.complete(ApiResult.Success(AppSiteSelectionResponseDto("account_a_site", "A 현장")))

        assertEquals(ApiResult.Failure(ApiError.SessionChanged), request.await())
        assertEquals("account_b_site", storage.selectedSiteId)
    }

    @Test
    fun `해제 성공 시 해당 현장이 계속 선택되어 있을 때만 선택을 비운다`() = runBlocking {
        val storage = InMemoryTokenStorage()
        val sessionManager = createAppAccountSession(storage)
        storage.saveSelectedSiteId("site_a")
        val repository =
            AppSiteLinkRepositoryImpl(
                appSiteLinkRemoteDataSource = FakeAppSiteLinkRemoteDataSource(unlink = { ApiResult.Success(Unit) }),
                sessionManager = sessionManager,
            )

        repository.unlinkSite("site_a")

        assertEquals(null, storage.selectedSiteId)
    }

    @Test
    fun `해제 중 새 현장을 선택하면 새 선택을 유지한다`() = runBlocking {
        val storage = InMemoryTokenStorage()
        val sessionManager = createAppAccountSession(storage)
        storage.saveSelectedSiteId("site_a")
        val unlinkGate = CompletableDeferred<ApiResult<Unit>>()
        val repository =
            AppSiteLinkRepositoryImpl(
                appSiteLinkRemoteDataSource = FakeAppSiteLinkRemoteDataSource(unlink = { unlinkGate.await() }),
                sessionManager = sessionManager,
            )

        val request = async { repository.unlinkSite("site_a") }
        yield()
        sessionManager.saveSelectedSiteIdForSession("site_b", sessionManager.getSnapshot())
        unlinkGate.complete(ApiResult.Success(Unit))
        request.await()

        assertEquals("site_b", storage.selectedSiteId)
    }

    @Test
    fun `목록 응답을 기다리는 중 바뀐 선택 현장은 그대로 유지한다`() = runBlocking {
        val storage = InMemoryTokenStorage()
        val sessionManager = createAppAccountSession(storage)
        storage.saveSelectedSiteId("site_a")
        val listGate = CompletableDeferred<ApiResult<AppLinkedSitesResponseDto>>()
        val repository =
            AppSiteLinkRepositoryImpl(
                appSiteLinkRemoteDataSource = FakeAppSiteLinkRemoteDataSource(linkedSites = { listGate.await() }),
                sessionManager = sessionManager,
            )

        val request = async { repository.getLinkedSites() }
        yield()
        sessionManager.saveSelectedSiteIdForSession("site_b", sessionManager.getSnapshot())
        listGate.complete(ApiResult.Success(AppLinkedSitesResponseDto(emptyList())))
        request.await()

        assertEquals("site_b", storage.selectedSiteId)
    }

    private fun createRepository(
        sessionManager: SessionManager,
        selectSite: suspend (String) -> ApiResult<AppSiteSelectionResponseDto>,
    ) = AppSiteLinkRepositoryImpl(
        appSiteLinkRemoteDataSource = FakeAppSiteLinkRemoteDataSource(selectSite),
        sessionManager = sessionManager,
    )

    private fun createAppAccountSession(storage: InMemoryTokenStorage) =
        SessionManager(storage, Dispatchers.Unconfined).apply {
            runBlocking {
                onLoginSucceeded(SessionToken("account-a-token", SessionRole.APP_ACCOUNT))
            }
        }

    private class FakeAppSiteLinkRemoteDataSource(
        private val selectSite: suspend (String) -> ApiResult<AppSiteSelectionResponseDto> = {
            ApiResult.Failure(ApiError.Unknown)
        },
        private val unlink: suspend (String) -> ApiResult<Unit> = { ApiResult.Success(Unit) },
        private val linkedSites: suspend () -> ApiResult<AppLinkedSitesResponseDto> = {
            ApiResult.Success(AppLinkedSitesResponseDto(listOf(AppLinkedSiteDto("site_a", "A 현장", null, "now"))))
        },
    ) : AppSiteLinkRemoteDataSource {
        override suspend fun getDashboard(siteId: String): ApiResult<AppSiteDashboardDto> = error("Not used")

        override suspend fun unlinkSite(siteId: String): ApiResult<Unit> = unlink.invoke(siteId)

        override suspend fun claim(
            token: String,
            managerPhone: String,
        ): ApiResult<AppSiteLinkClaimResponseDto> = error("Not used")

        override suspend fun getLinkedSites(): ApiResult<AppLinkedSitesResponseDto> = linkedSites.invoke()

        override suspend fun selectSite(siteId: String): ApiResult<AppSiteSelectionResponseDto> =
            selectSite.invoke(siteId)
    }
}
