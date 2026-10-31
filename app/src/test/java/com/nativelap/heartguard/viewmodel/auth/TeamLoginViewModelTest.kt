package com.nativelap.heartguard.viewmodel.auth

import com.nativelap.heartguard.domain.auth.model.TeamLoginResult
import com.nativelap.heartguard.domain.auth.repository.TeamAuthRepository
import com.nativelap.heartguard.domain.auth.usecase.TeamLoginUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TeamLoginViewModelTest {
    @Test
    fun `login results map to the existing failure states`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val repository = FakeTeamAuthRepository()
            val viewModel = TeamLoginViewModel(TeamLoginUseCase(repository))

            try {
                repository.nextResult = TeamLoginResult.InvalidCredentials
                viewModel.login(email = "", password = "")
                runCurrent()
                assertEquals(TeamLoginFailure.INVALID_CREDENTIALS, viewModel.uiState.value.failure)

                repository.nextResult = TeamLoginResult.Failure
                viewModel.login(email = "", password = "")
                runCurrent()
                assertEquals(TeamLoginFailure.GENERIC, viewModel.uiState.value.failure)

                repository.nextResult = TeamLoginResult.AccountDisabled
                viewModel.login(email = "", password = "")
                runCurrent()
                assertEquals(TeamLoginFailure.ACCOUNT_DISABLED, viewModel.uiState.value.failure)

                repository.nextResult = TeamLoginResult.RateLimited
                viewModel.login(email = "", password = "")
                runCurrent()
                assertEquals(TeamLoginFailure.RATE_LIMITED, viewModel.uiState.value.failure)

                repository.nextResult = TeamLoginResult.Success
                viewModel.login(email = "", password = "")
                runCurrent()
                assertNull(viewModel.uiState.value.failure)
                assertFalse(viewModel.uiState.value.isSubmitting)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `submitting rejects duplicate login requests until the first completes`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val loginResult = CompletableDeferred<TeamLoginResult>()
            val repository = FakeTeamAuthRepository(blockingResult = loginResult)
            val viewModel = TeamLoginViewModel(TeamLoginUseCase(repository))

            try {
                viewModel.login(email = "", password = "")
                assertTrue(viewModel.uiState.value.isSubmitting)

                viewModel.login(email = "", password = "")
                runCurrent()
                assertEquals(1, repository.loginCallCount)
                assertTrue(viewModel.uiState.value.isSubmitting)

                loginResult.complete(TeamLoginResult.Success)
                runCurrent()

                assertFalse(viewModel.uiState.value.isSubmitting)
                assertNull(viewModel.uiState.value.failure)
            } finally {
                Dispatchers.resetMain()
            }
        }

    private class FakeTeamAuthRepository(
        var nextResult: TeamLoginResult = TeamLoginResult.Failure,
        private val blockingResult: CompletableDeferred<TeamLoginResult>? = null,
    ) : TeamAuthRepository {
        var loginCallCount = 0
            private set

        override suspend fun login(
            email: String,
            password: String,
        ): TeamLoginResult {
            loginCallCount += 1
            return blockingResult?.await() ?: nextResult
        }

        override suspend fun logout() = Unit
    }
}
