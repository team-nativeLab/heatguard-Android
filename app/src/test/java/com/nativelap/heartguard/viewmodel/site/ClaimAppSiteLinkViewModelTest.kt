package com.nativelap.heartguard.viewmodel.site

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.PendingAppLink
import com.nativelap.heartguard.domain.site.model.AppLinkedSite
import com.nativelap.heartguard.domain.site.model.AppSelectedSite
import com.nativelap.heartguard.domain.site.model.AppSiteLinkClaim
import com.nativelap.heartguard.domain.site.repository.AppSiteLinkRepository
import com.nativelap.heartguard.domain.site.usecase.ClaimAppSiteLinkUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClaimAppSiteLinkViewModelTest {
    @Test
    fun lateSuccessForPreviousLinkDoesNotCompleteNewLink() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val response = CompletableDeferred<ApiResult<AppSiteLinkClaim>>()
                val repository = FakeAppSiteLinkRepository(response)
                val viewModel = ClaimAppSiteLinkViewModel(ClaimAppSiteLinkUseCase(repository))
                val firstLink = PendingAppLink("first-link", 1L)
                val secondLink = PendingAppLink("second-link", 2L)

                viewModel.activateLink(firstLink)
                runCurrent()
                viewModel.claim(firstLink, "010-1234-5678")
                runCurrent()

                viewModel.activateLink(secondLink)
                response.complete(ApiResult.Success(AppSiteLinkClaim("site-1", "현장", "2026-10-23")))
                advanceUntilIdle()

                assertEquals(2L, viewModel.uiState.value.linkId)
                assertNull(viewModel.uiState.value.completedLinkId)
                assertFalse(viewModel.uiState.value.isSubmitting)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun successIsAssociatedWithClaimedLinkAndPhoneIsNormalized() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val repository = FakeAppSiteLinkRepository()
                val viewModel = ClaimAppSiteLinkViewModel(ClaimAppSiteLinkUseCase(repository))

                val pendingAppLink = PendingAppLink("link-token", 10L)
                viewModel.activateLink(pendingAppLink)
                runCurrent()
                viewModel.claim(pendingAppLink, "010-1234-5678")
                advanceUntilIdle()

                assertEquals("01012345678", repository.managerPhone)
                assertEquals(10L, viewModel.uiState.value.completedLinkId)
                assertEquals(10L, viewModel.uiState.value.linkId)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun appAccountEntryClearsPreviousSessionRequiredFailure() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val pendingAppLink = PendingAppLink("link-token", 20L)
                val repository =
                    FakeAppSiteLinkRepository(
                        defaultClaimResult = ApiResult.Failure(ApiError.SessionChanged),
                    )
                val viewModel = ClaimAppSiteLinkViewModel(ClaimAppSiteLinkUseCase(repository))

                viewModel.activateLink(pendingAppLink)
                viewModel.claim(pendingAppLink, "01012345678")
                advanceUntilIdle()

                assertEquals(ClaimAppSiteLinkFailure.SESSION_REQUIRED, viewModel.uiState.value.failure)

                viewModel.activateLink(pendingAppLink, clearSessionRequiredFailure = true)

                assertNull(viewModel.uiState.value.failure)
                assertEquals(20L, viewModel.uiState.value.linkId)
            } finally {
                Dispatchers.resetMain()
            }
        }
}

private class FakeAppSiteLinkRepository(
    private val claimResponse: CompletableDeferred<ApiResult<AppSiteLinkClaim>>? = null,
    private val defaultClaimResult: ApiResult<AppSiteLinkClaim> =
        ApiResult.Success(AppSiteLinkClaim("site-1", "현장", "2026-10-23")),
) : AppSiteLinkRepository {
    var managerPhone: String? = null
        private set

    override suspend fun claim(
        token: String,
        managerPhone: String,
    ): ApiResult<AppSiteLinkClaim> {
        this.managerPhone = managerPhone
        return claimResponse?.await() ?: defaultClaimResult
    }

    override suspend fun getLinkedSites(): ApiResult<List<AppLinkedSite>> = ApiResult.Success(emptyList())

    override suspend fun selectSite(siteId: String): ApiResult<AppSelectedSite> = error("Not used by claim tests")

    override fun getSelectedSiteId(): String? = null
}
