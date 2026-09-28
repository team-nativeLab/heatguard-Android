package com.nativelap.heartguard.viewmodel.menu

import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionState
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.domain.auth.model.TeamLoginResult
import com.nativelap.heartguard.domain.auth.repository.TeamAuthRepository
import com.nativelap.heartguard.domain.auth.usecase.TeamLogoutUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MenuDrawerViewModelTest {
    @Test
    fun logoutCallsServerBeforeClearingLocalTokenAndIgnoresRepeatedTaps() = runTest {
        val mainDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(mainDispatcher)
        try {
            val tokenStorage = FakeTokenStorage(accessToken = "access-token")
            val sessionManager = SessionManager(
                tokenStorage = tokenStorage,
                ioDispatcher = StandardTestDispatcher(testScheduler),
            )
            sessionManager.initialize()

            val remoteLogoutStarted = CompletableDeferred<Unit>()
            val continueRemoteLogout = CompletableDeferred<Unit>()
            var logoutRequestCount = 0
            val repository = FakeTeamAuthRepository {
                logoutRequestCount += 1
                assertEquals("access-token", tokenStorage.accessToken)
                remoteLogoutStarted.complete(Unit)
                continueRemoteLogout.await()
            }
            val viewModel = MenuDrawerViewModel(
                teamLogoutUseCase = TeamLogoutUseCase(repository),
                sessionManager = sessionManager,
            )

            viewModel.logout()
            viewModel.logout()
            runCurrent()

            assertEquals(1, logoutRequestCount)
            assertEquals("access-token", tokenStorage.accessToken)
            assertEquals(SessionState.Authenticated, sessionManager.sessionState.value)

            continueRemoteLogout.complete(Unit)
            advanceUntilIdle()

            assertNull(tokenStorage.accessToken)
            assertEquals(SessionState.Unauthenticated, sessionManager.sessionState.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun cancelledRemoteLogoutStillClearsLocalSession() = runTest {
        val mainDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(mainDispatcher)
        try {
            val tokenStorage = FakeTokenStorage(accessToken = "access-token")
            val sessionManager = SessionManager(
                tokenStorage = tokenStorage,
                ioDispatcher = StandardTestDispatcher(testScheduler),
            )
            sessionManager.initialize()

            val remoteLogoutStarted = CompletableDeferred<Unit>()
            val repository = FakeTeamAuthRepository {
                remoteLogoutStarted.complete(Unit)
                CompletableDeferred<Unit>().await()
            }
            val viewModel = MenuDrawerViewModel(
                teamLogoutUseCase = TeamLogoutUseCase(repository),
                sessionManager = sessionManager,
            )

            viewModel.logout()
            runCurrent()
            remoteLogoutStarted.await()

            viewModel.viewModelScope.cancel()
            advanceUntilIdle()

            assertNull(tokenStorage.accessToken)
            assertEquals(SessionState.Unauthenticated, sessionManager.sessionState.value)
        } finally {
            Dispatchers.resetMain()
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

    private class FakeTeamAuthRepository(
        private val onLogout: suspend () -> Unit,
    ) : TeamAuthRepository {
        override suspend fun login(email: String, password: String): TeamLoginResult =
            TeamLoginResult.Failure

        override suspend fun logout() {
            onLogout()
        }
    }
}
