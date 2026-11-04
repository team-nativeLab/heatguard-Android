package com.nativelap.heartguard.navigation

import androidx.lifecycle.ViewModelStore
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionRole
import com.nativelap.heartguard.core.session.SessionState
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.domain.profile.model.PasswordChangeResult
import com.nativelap.heartguard.domain.profile.model.ProfilePhoto
import com.nativelap.heartguard.domain.profile.model.ProfileUpdateResult
import com.nativelap.heartguard.domain.profile.model.WorkerProfile
import com.nativelap.heartguard.domain.profile.repository.WorkerProfileRepository
import com.nativelap.heartguard.domain.profile.usecase.GetWorkerProfileUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HeartGuardSessionViewModelTest {
    @Test
    fun `startup check completes immediately without profile request when no token is stored`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val repository = FakeWorkerProfileRepository()
            val sessionManager = SessionManager(TestTokenStorage(storedAccessToken = null), dispatcher)
            val viewModelStore = ViewModelStore()

            try {
                val viewModel = createViewModel(repository, sessionManager, viewModelStore)
                runCurrent()

                assertTrue(viewModel.isStartupCheckComplete.value)
                assertEquals(0, repository.profileRequestCount)
                assertEquals(SessionState.Unauthenticated, sessionManager.sessionState.value)
            } finally {
                viewModelStore.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `startup check does not request profile for a newly logged in session`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val repository = FakeWorkerProfileRepository()
            val sessionManager = SessionManager(TestTokenStorage(storedAccessToken = null), dispatcher)
            sessionManager.initialize()
            sessionManager.onLoginSucceeded(
                SessionToken(
                    accessToken = "new-login-token",
                    role = SessionRole.TEAM_MEMBER,
                ),
            )
            val viewModelStore = ViewModelStore()

            try {
                val viewModel = createViewModel(repository, sessionManager, viewModelStore)
                runCurrent()

                assertTrue(viewModel.isStartupCheckComplete.value)
                assertEquals(0, repository.profileRequestCount)
                assertEquals(SessionState.Authenticated, sessionManager.sessionState.value)
            } finally {
                viewModelStore.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `restored session stays authenticated after successful profile check`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val profileGate = CompletableDeferred<ApiResult<WorkerProfile>>()
            val repository = FakeWorkerProfileRepository()
            repository.profileRequest = {
                profileGate.await()
            }
            val sessionManager = createRestoredSessionManager(dispatcher)
            val viewModelStore = ViewModelStore()

            try {
                val viewModel = createViewModel(repository, sessionManager, viewModelStore)
                runCurrent()

                assertEquals(1, repository.profileRequestCount)
                assertFalse(viewModel.isStartupCheckComplete.value)

                profileGate.complete(ApiResult.Success(testProfile))
                runCurrent()

                assertTrue(viewModel.isStartupCheckComplete.value)
                assertEquals(SessionState.Authenticated, sessionManager.sessionState.value)
            } finally {
                viewModelStore.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `restored session ends when profile check is rejected with 401`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val repository = FakeWorkerProfileRepository()
            val sessionManager = createRestoredSessionManager(dispatcher)
            repository.profileRequest = {
                // 실제 앱에서는 BearerTokenAuthenticator가 401에서 세션을 끝낸다.
                sessionManager.expireSession()
                ApiResult.Failure(ApiError.Http(statusCode = 401))
            }
            val viewModelStore = ViewModelStore()

            try {
                val viewModel = createViewModel(repository, sessionManager, viewModelStore)
                runCurrent()

                assertTrue(viewModel.isStartupCheckComplete.value)
                assertEquals(SessionState.Unauthenticated, sessionManager.sessionState.value)
            } finally {
                viewModelStore.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `restored session ends when app server rejects profile check with 403`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val repository = FakeWorkerProfileRepository()
            repository.profileRequest = {
                ApiResult.Failure(
                    ApiError.Http(
                        statusCode = 403,
                        errorCode = APP_SERVER_FORBIDDEN_ERROR_CODE,
                    ),
                )
            }
            val sessionManager = createRestoredSessionManager(dispatcher)
            val viewModelStore = ViewModelStore()

            try {
                val viewModel = createViewModel(repository, sessionManager, viewModelStore)
                runCurrent()

                assertTrue(viewModel.isStartupCheckComplete.value)
                assertEquals(SessionState.Unauthenticated, sessionManager.sessionState.value)
            } finally {
                viewModelStore.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `restored session stays authenticated when 403 has no app server error code`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val repository = FakeWorkerProfileRepository()
            repository.profileRequest = {
                ApiResult.Failure(ApiError.Http(statusCode = 403))
            }
            val sessionManager = createRestoredSessionManager(dispatcher)
            val viewModelStore = ViewModelStore()

            try {
                val viewModel = createViewModel(repository, sessionManager, viewModelStore)
                runCurrent()

                assertTrue(viewModel.isStartupCheckComplete.value)
                assertEquals(SessionState.Authenticated, sessionManager.sessionState.value)
            } finally {
                viewModelStore.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `app server 403 does not expire a session that changed during the check`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val repository = FakeWorkerProfileRepository()
            val sessionManager = createRestoredSessionManager(dispatcher)
            repository.profileRequest = {
                sessionManager.onLoginSucceeded(
                    SessionToken(
                        accessToken = "next-login-token",
                        role = SessionRole.TEAM_MEMBER,
                    ),
                )
                ApiResult.Failure(
                    ApiError.Http(
                        statusCode = 403,
                        errorCode = APP_SERVER_FORBIDDEN_ERROR_CODE,
                    ),
                )
            }
            val viewModelStore = ViewModelStore()

            try {
                val viewModel = createViewModel(repository, sessionManager, viewModelStore)
                runCurrent()

                assertTrue(viewModel.isStartupCheckComplete.value)
                assertEquals(SessionState.Authenticated, sessionManager.sessionState.value)
                assertEquals("next-login-token", sessionManager.getSnapshot().accessToken)
            } finally {
                viewModelStore.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `clearing the view model during the check lets the next view model check again`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val firstRequestGate = CompletableDeferred<ApiResult<WorkerProfile>>()
            val repository = FakeWorkerProfileRepository()
            repository.profileRequest = {
                if (repository.profileRequestCount == 1) {
                    firstRequestGate.await()
                } else {
                    ApiResult.Success(testProfile)
                }
            }
            val sessionManager = createRestoredSessionManager(dispatcher)
            val firstViewModelStore = ViewModelStore()
            val secondViewModelStore = ViewModelStore()

            try {
                val firstViewModel = createViewModel(repository, sessionManager, firstViewModelStore)
                runCurrent()
                assertEquals(1, repository.profileRequestCount)
                assertFalse(firstViewModel.isStartupCheckComplete.value)

                firstViewModelStore.clear()
                runCurrent()

                val recreatedViewModel = createViewModel(repository, sessionManager, secondViewModelStore)
                runCurrent()

                assertEquals(2, repository.profileRequestCount)
                assertTrue(recreatedViewModel.isStartupCheckComplete.value)
                assertEquals(SessionState.Authenticated, sessionManager.sessionState.value)
            } finally {
                firstViewModelStore.clear()
                secondViewModelStore.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `restored session stays authenticated when profile check fails by network`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val repository = FakeWorkerProfileRepository()
            repository.profileRequest = {
                ApiResult.Failure(ApiError.Network)
            }
            val sessionManager = createRestoredSessionManager(dispatcher)
            val viewModelStore = ViewModelStore()

            try {
                val viewModel = createViewModel(repository, sessionManager, viewModelStore)
                runCurrent()

                assertTrue(viewModel.isStartupCheckComplete.value)
                assertEquals(SessionState.Authenticated, sessionManager.sessionState.value)
            } finally {
                viewModelStore.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `restored session stays authenticated when profile check fails by server error`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val repository = FakeWorkerProfileRepository()
            repository.profileRequest = {
                ApiResult.Failure(ApiError.Http(statusCode = 503))
            }
            val sessionManager = createRestoredSessionManager(dispatcher)
            val viewModelStore = ViewModelStore()

            try {
                val viewModel = createViewModel(repository, sessionManager, viewModelStore)
                runCurrent()

                assertTrue(viewModel.isStartupCheckComplete.value)
                assertEquals(SessionState.Authenticated, sessionManager.sessionState.value)
            } finally {
                viewModelStore.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `startup check completes and cancels profile request after five seconds`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val neverCompletingGate = CompletableDeferred<ApiResult<WorkerProfile>>()
            var isProfileRequestCancelled = false
            val repository = FakeWorkerProfileRepository()
            repository.profileRequest = {
                try {
                    neverCompletingGate.await()
                } catch (cancellationException: CancellationException) {
                    isProfileRequestCancelled = true
                    throw cancellationException
                }
            }
            val sessionManager = createRestoredSessionManager(dispatcher)
            val viewModelStore = ViewModelStore()

            try {
                val viewModel = createViewModel(repository, sessionManager, viewModelStore)
                runCurrent()
                advanceTimeBy(4_999L)
                runCurrent()

                assertFalse(viewModel.isStartupCheckComplete.value)

                advanceTimeBy(2L)
                runCurrent()

                assertTrue(viewModel.isStartupCheckComplete.value)
                assertTrue(isProfileRequestCancelled)
                assertEquals(SessionState.Authenticated, sessionManager.sessionState.value)
            } finally {
                viewModelStore.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `startup check passes the restored session generation to the profile request`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val requestedGenerations = mutableListOf<Long?>()
            val repository = FakeWorkerProfileRepository()
            repository.profileRequest = { expectedSessionGeneration ->
                requestedGenerations += expectedSessionGeneration
                ApiResult.Success(testProfile)
            }
            val sessionManager = createRestoredSessionManager(dispatcher)
            val viewModelStore = ViewModelStore()

            try {
                createViewModel(repository, sessionManager, viewModelStore)
                runCurrent()

                assertEquals(
                    listOf<Long?>(sessionManager.getSnapshot().generation),
                    requestedGenerations,
                )
            } finally {
                viewModelStore.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `recreating the view model in the same process does not repeat the startup check`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val repository = FakeWorkerProfileRepository()
            val sessionManager = createRestoredSessionManager(dispatcher)
            val firstViewModelStore = ViewModelStore()
            val secondViewModelStore = ViewModelStore()

            try {
                createViewModel(repository, sessionManager, firstViewModelStore)
                runCurrent()
                firstViewModelStore.clear()

                val recreatedViewModel = createViewModel(repository, sessionManager, secondViewModelStore)
                runCurrent()

                assertEquals(1, repository.profileRequestCount)
                assertTrue(recreatedViewModel.isStartupCheckComplete.value)
            } finally {
                firstViewModelStore.clear()
                secondViewModelStore.clear()
                Dispatchers.resetMain()
            }
        }

    private fun createRestoredSessionManager(dispatcher: CoroutineDispatcher): SessionManager =
        SessionManager(
            TestTokenStorage(storedAccessToken = "restored-session-token"),
            dispatcher,
        )

    private fun createViewModel(
        repository: FakeWorkerProfileRepository,
        sessionManager: SessionManager,
        viewModelStore: ViewModelStore,
    ): HeartGuardSessionViewModel {
        val viewModel =
            HeartGuardSessionViewModel(
                sessionManager = sessionManager,
                getWorkerProfileUseCase = GetWorkerProfileUseCase(repository),
            )
        viewModelStore.put("heart-guard-session", viewModel)
        return viewModel
    }

    private class TestTokenStorage(
        private val storedAccessToken: String?,
    ) : TokenStorage {
        override fun readAccessToken(): String? = storedAccessToken

        override fun saveAccessToken(accessToken: String) = Unit

        override fun clear() = Unit
    }

    private class FakeWorkerProfileRepository : WorkerProfileRepository {
        var profileRequestCount = 0
            private set
        var profileRequest: suspend (Long?) -> ApiResult<WorkerProfile> = {
            ApiResult.Success(testProfile)
        }

        override suspend fun getWorkerProfile(expectedSessionGeneration: Long?): ApiResult<WorkerProfile> {
            profileRequestCount += 1
            return profileRequest(expectedSessionGeneration)
        }

        override suspend fun updateWorkerName(
            name: String,
            version: Long?,
            expectedSessionGeneration: Long?,
        ): ProfileUpdateResult = ProfileUpdateResult.Failure

        override suspend fun changePassword(
            currentPassword: String,
            newPassword: String,
        ): PasswordChangeResult = PasswordChangeResult.Failure

        override suspend fun getProfilePhoto(expectedSessionGeneration: Long): ApiResult<ProfilePhoto?> =
            ApiResult.Success(null)

        override suspend fun uploadProfilePhoto(
            photoUri: String,
            expectedSessionGeneration: Long,
        ): ApiResult<ProfilePhoto> = ApiResult.Failure(ApiError.Unknown)

        override suspend fun deleteProfilePhoto(expectedSessionGeneration: Long): ApiResult<Unit> =
            ApiResult.Failure(ApiError.Unknown)
    }

    private companion object {
        const val APP_SERVER_FORBIDDEN_ERROR_CODE = "ACCOUNT_DISABLED"

        val testProfile =
            WorkerProfile(
                userId = "worker-1",
                name = "현장 작업자",
                email = "worker@example.test",
            )
    }
}
