package com.nativelap.heartguard.viewmodel.account

import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionState
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.domain.account.model.WithdrawAccountResult
import com.nativelap.heartguard.domain.account.model.WithdrawReason
import com.nativelap.heartguard.domain.account.repository.AccountRepository
import com.nativelap.heartguard.domain.account.usecase.WithdrawAccountUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class WithdrawViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `선택한 탈퇴 사유를 비밀번호와 함께 보낸다`() =
        runTest {
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
    fun `사유를 고르지 않으면 null로 보낸다`() =
        runTest {
            val repository = FakeAccountRepository()
            val viewModel = createViewModel(repository)

            viewModel.updatePassword("current-password")
            viewModel.updateAgreement(true)
            viewModel.withdraw()
            advanceUntilIdle()

            assertEquals(listOf("current-password" to null), repository.requests)
        }

    @Test
    fun lateWithdrawalResponseDoesNotClearNewLogin() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val storage = FakeTokenStorage("first-token")
            val sessionManager = SessionManager(storage, StandardTestDispatcher(testScheduler))
            sessionManager.initialize()
            val response = CompletableDeferred<WithdrawAccountResult>()
            val repository = FakeAccountRepository { response.await() }
            val viewModel = WithdrawViewModel(WithdrawAccountUseCase(repository), sessionManager)
            viewModel.updatePassword("password")
            viewModel.updateAgreement(true)
            viewModel.withdraw()
            runCurrent()
            sessionManager.onLoginSucceeded(SessionToken("new-token"))
            response.complete(WithdrawAccountResult.Success)
            advanceUntilIdle()

            assertEquals("new-token", storage.accessToken)
            assertEquals(SessionState.Authenticated, sessionManager.sessionState.value)
            assertEquals(WithdrawSubmissionState.Idle, viewModel.uiState.value.submissionState)
            assertEquals("", viewModel.password.value)
        }

    @Test
    fun completionFromOldWithdrawalDoesNotExpireNewLogin() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val storage = FakeTokenStorage("first-token")
            val sessionManager = SessionManager(storage, StandardTestDispatcher(testScheduler))
            sessionManager.initialize()
            val viewModel = WithdrawViewModel(WithdrawAccountUseCase(FakeAccountRepository()), sessionManager)
            viewModel.updatePassword("password")
            viewModel.updateAgreement(true)
            viewModel.withdraw()
            advanceUntilIdle()
            assertEquals(null, storage.accessToken)
            sessionManager.onLoginSucceeded(SessionToken("new-token"))
            viewModel.finishWithdrawal()
            advanceUntilIdle()

            assertEquals("new-token", storage.accessToken)
            assertEquals(SessionState.Authenticated, sessionManager.sessionState.value)
        }

    @Test
    fun finishingWithdrawalAfterLosingViewModelStateStillEndsWithdrawnSession() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val storage = FakeTokenStorage("first-token")
            val sessionManager = SessionManager(storage, StandardTestDispatcher(testScheduler))
            sessionManager.initialize()
            val viewModel = WithdrawViewModel(WithdrawAccountUseCase(FakeAccountRepository()), sessionManager)
            viewModel.updatePassword("password")
            viewModel.updateAgreement(true)
            viewModel.withdraw()
            advanceUntilIdle()
            // Activity 재생성 등으로 흐름 상태를 잃은 경우다.
            viewModel.reset()

            viewModel.finishWithdrawal()
            advanceUntilIdle()

            assertEquals(SessionState.Unauthenticated, sessionManager.sessionState.value)
        }

    @Test
    fun ownershipLostWhileWaitingForStorageLockClearsOldSubmission() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val storage = FakeTokenStorage("first-token")
            val sessionManager = SessionManager(storage, Dispatchers.IO)
            sessionManager.initialize()
            val response = CompletableDeferred<WithdrawAccountResult>()
            val viewModel =
                WithdrawViewModel(
                    WithdrawAccountUseCase(FakeAccountRepository { response.await() }),
                    sessionManager,
                )
            viewModel.updatePassword("password")
            viewModel.updateAgreement(true)
            viewModel.withdraw()
            runCurrent()

            val loginWriteStarted = CompletableDeferred<Unit>()
            val releaseLoginWrite = CountDownLatch(1)
            storage.beforeSave = {
                loginWriteStarted.complete(Unit)
                check(releaseLoginWrite.await(5, TimeUnit.SECONDS))
            }
            try {
                val loginJob =
                    launch(Dispatchers.IO) {
                        sessionManager.onLoginSucceeded(SessionToken("new-token"))
                    }
                loginWriteStarted.await()
                response.complete(WithdrawAccountResult.Success)
                runCurrent()
                assertEquals(WithdrawSubmissionState.Submitting, viewModel.uiState.value.submissionState)
                releaseLoginWrite.countDown()
                loginJob.join()
                advanceUntilIdle()

                assertEquals("new-token", storage.accessToken)
                assertEquals(WithdrawSubmissionState.Idle, viewModel.uiState.value.submissionState)
                assertEquals("", viewModel.password.value)
            } finally {
                releaseLoginWrite.countDown()
            }
        }

    private suspend fun TestScope.createViewModel(repository: FakeAccountRepository): WithdrawViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val sessionManager =
            SessionManager(
                tokenStorage = FakeTokenStorage(accessToken = "access-token"),
                ioDispatcher = StandardTestDispatcher(testScheduler),
            )
        sessionManager.initialize()
        return WithdrawViewModel(
            withdrawAccountUseCase = WithdrawAccountUseCase(repository),
            sessionManager = sessionManager,
        )
    }

    private class FakeAccountRepository(
        private val respond: suspend () -> WithdrawAccountResult = { WithdrawAccountResult.Success },
    ) : AccountRepository {
        val requests = mutableListOf<Pair<String, WithdrawReason?>>()

        override suspend fun withdraw(
            currentPassword: String,
            reason: WithdrawReason?,
        ): WithdrawAccountResult {
            requests += currentPassword to reason
            return respond()
        }
    }

    private class FakeTokenStorage(
        var accessToken: String?,
    ) : TokenStorage {
        var beforeSave: (() -> Unit)? = null

        override fun readAccessToken(): String? = accessToken

        override fun saveAccessToken(accessToken: String) {
            beforeSave?.invoke()
            this.accessToken = accessToken
        }

        override fun clear() {
            accessToken = null
        }
    }
}
