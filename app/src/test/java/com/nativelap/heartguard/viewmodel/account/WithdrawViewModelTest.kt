package com.nativelap.heartguard.viewmodel.account

import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.domain.account.model.WithdrawAccountResult
import com.nativelap.heartguard.domain.account.model.WithdrawReason
import com.nativelap.heartguard.domain.account.repository.AccountRepository
import com.nativelap.heartguard.domain.account.usecase.WithdrawAccountUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WithdrawViewModelTest {

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `선택한 탈퇴 사유를 비밀번호와 함께 보낸다`() = runTest {
        val repository = FakeAccountRepository()
        val viewModel = createViewModel(repository)

        viewModel.selectReason(WithdrawReason.FIELD_WORK_ENDED)
        viewModel.updatePassword("current-password")
        viewModel.updateAgreement(true)
        viewModel.withdraw()
        advanceUntilIdle()

        assertEquals(listOf("current-password" to WithdrawReason.FIELD_WORK_ENDED), repository.requests)
        assertEquals(WithdrawSubmissionState.Succeeded, viewModel.uiState.value.submissionState)
    }

    @Test
    fun `사유를 고르지 않으면 null로 보낸다`() = runTest {
        val repository = FakeAccountRepository()
        val viewModel = createViewModel(repository)

        viewModel.updatePassword("current-password")
        viewModel.updateAgreement(true)
        viewModel.withdraw()
        advanceUntilIdle()

        assertEquals(listOf("current-password" to null), repository.requests)
    }

    private suspend fun TestScope.createViewModel(repository: FakeAccountRepository): WithdrawViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val sessionManager = SessionManager(
            tokenStorage = FakeTokenStorage(accessToken = "access-token"),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        sessionManager.initialize()
        return WithdrawViewModel(
            withdrawAccountUseCase = WithdrawAccountUseCase(repository),
            sessionManager = sessionManager,
        )
    }

    private class FakeAccountRepository : AccountRepository {
        val requests = mutableListOf<Pair<String, WithdrawReason?>>()

        override suspend fun withdraw(
            currentPassword: String,
            reason: WithdrawReason?,
        ): WithdrawAccountResult {
            requests += currentPassword to reason
            return WithdrawAccountResult.Success
        }
    }

    private class FakeTokenStorage(
        var accessToken: String?,
    ) : TokenStorage {
        override fun readAccessToken(): String? = accessToken

        override fun saveAccessToken(accessToken: String) {
            this.accessToken = accessToken
        }

        override fun clear() {
            accessToken = null
        }
    }
}
