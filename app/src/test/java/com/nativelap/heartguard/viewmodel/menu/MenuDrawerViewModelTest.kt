package com.nativelap.heartguard.viewmodel.menu

import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionState
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.domain.auth.model.TeamLoginResult
import com.nativelap.heartguard.domain.auth.repository.TeamAuthRepository
import com.nativelap.heartguard.domain.auth.usecase.TeamLogoutUseCase
import com.nativelap.heartguard.domain.profile.model.PasswordChangeResult
import com.nativelap.heartguard.domain.profile.model.ProfileUpdateResult
import com.nativelap.heartguard.domain.profile.model.WorkerProfile
import com.nativelap.heartguard.domain.profile.repository.WorkerProfileRepository
import com.nativelap.heartguard.domain.profile.usecase.GetWorkerProfileUseCase
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
                getWorkerProfileUseCase = GetWorkerProfileUseCase(FakeWorkerProfileRepository()),
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
                getWorkerProfileUseCase = GetWorkerProfileUseCase(FakeWorkerProfileRepository()),
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

    @Test
    fun loadedProfileIsClearedWhenSessionEnds() = runTest {
        val mainDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(mainDispatcher)
        try {
            val sessionManager = SessionManager(
                tokenStorage = FakeTokenStorage(accessToken = "access-token"),
                ioDispatcher = StandardTestDispatcher(testScheduler),
            )
            sessionManager.initialize()
            val viewModel = MenuDrawerViewModel(
                getWorkerProfileUseCase = GetWorkerProfileUseCase(FakeWorkerProfileRepository()),
                teamLogoutUseCase = TeamLogoutUseCase(FakeTeamAuthRepository {}),
                sessionManager = sessionManager,
            )

            viewModel.loadProfile()
            advanceUntilIdle()

            assertEquals("홍길동", viewModel.profile.value.userName)
            assertEquals("worker01", viewModel.profile.value.email)

            sessionManager.expireSession()
            advanceUntilIdle()

            assertEquals(MenuDrawerProfileUiModel(), viewModel.profile.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeWorkerProfileRepository : WorkerProfileRepository {
        override suspend fun getWorkerProfile(): ApiResult<WorkerProfile> = ApiResult.Success(
            WorkerProfile(
                userId = "usr_01",
                name = "홍길동",
                email = "worker01",
            ),
        )

        override suspend fun updateWorkerName(
            name: String,
            version: Long?,
        ): ProfileUpdateResult = ProfileUpdateResult.Failure

        override suspend fun changePassword(
            currentPassword: String,
            newPassword: String,
        ): PasswordChangeResult = PasswordChangeResult.Failure
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
