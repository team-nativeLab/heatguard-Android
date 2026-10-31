package com.nativelap.heartguard.viewmodel.menu

import androidx.lifecycle.ViewModelStore
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionRole
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.domain.auth.model.TeamLoginResult
import com.nativelap.heartguard.domain.auth.repository.TeamAuthRepository
import com.nativelap.heartguard.domain.auth.usecase.TeamLogoutUseCase
import com.nativelap.heartguard.domain.profile.model.PasswordChangeResult
import com.nativelap.heartguard.domain.profile.model.ProfilePhoto
import com.nativelap.heartguard.domain.profile.model.ProfileUpdateResult
import com.nativelap.heartguard.domain.profile.model.WorkerProfile
import com.nativelap.heartguard.domain.profile.repository.WorkerProfileRepository
import com.nativelap.heartguard.domain.profile.usecase.GetWorkerProfilePhotoUseCase
import com.nativelap.heartguard.domain.profile.usecase.GetWorkerProfileUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MenuDrawerViewModelTest {
    @Test
    fun `profile response is applied when photo request fails`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeWorkerProfileRepository()
            repository.photoRequest = {
                ApiResult.Failure(ApiError.Network)
            }
            val sessionManager = createAuthenticatedSession(StandardTestDispatcher(testScheduler))
            val viewModelStore = ViewModelStore()

            try {
                val viewModel = createViewModel(repository, sessionManager, viewModelStore)
                runCurrent()

                viewModel.loadProfile()
                runCurrent()

                assertEquals("현장 작업자", viewModel.profile.value.userName)
                assertEquals("worker@example.test", viewModel.profile.value.email)
                assertNull(viewModel.profile.value.photoUrl)
                assertEquals(1, repository.profileRequestCount)
                assertEquals(1, repository.photoRequestCount)
            } finally {
                viewModelStore.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `profile and photo requests start concurrently`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val profileStarted = CompletableDeferred<Unit>()
            val photoStarted = CompletableDeferred<Unit>()
            val releaseRequests = CompletableDeferred<Unit>()
            val repository = FakeWorkerProfileRepository()
            repository.profileRequest = {
                profileStarted.complete(Unit)
                releaseRequests.await()
                ApiResult.Success(testProfile)
            }
            repository.photoRequest = {
                photoStarted.complete(Unit)
                releaseRequests.await()
                ApiResult.Success(ProfilePhoto("https://images.example.test/profile", null))
            }
            val sessionManager = createAuthenticatedSession(StandardTestDispatcher(testScheduler))
            val viewModelStore = ViewModelStore()

            try {
                val viewModel = createViewModel(repository, sessionManager, viewModelStore)
                runCurrent()

                viewModel.loadProfile()
                runCurrent()

                assertTrue(profileStarted.isCompleted)
                assertTrue(photoStarted.isCompleted)
                assertNull(viewModel.profile.value.userName)

                releaseRequests.complete(Unit)
                runCurrent()

                assertEquals(testProfile.name, viewModel.profile.value.userName)
                assertEquals("https://images.example.test/profile", viewModel.profile.value.photoUrl)
            } finally {
                viewModelStore.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `response from previous session is not applied after account changes`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeWorkerProfileRepository()
            var sessionManager: SessionManager? = null
            repository.photoRequest = {
                sessionManager?.onLoginSucceeded(
                    SessionToken(
                        accessToken = "second-account-token",
                        role = SessionRole.TEAM_MEMBER,
                    ),
                )
                ApiResult.Success(ProfilePhoto("https://images.example.test/previous-account", null))
            }
            val activeSessionManager = createAuthenticatedSession(StandardTestDispatcher(testScheduler))
            sessionManager = activeSessionManager
            val viewModelStore = ViewModelStore()

            try {
                val viewModel = createViewModel(repository, activeSessionManager, viewModelStore)
                runCurrent()

                viewModel.loadProfile()
                runCurrent()

                assertEquals(3L, activeSessionManager.getSnapshot().generation)
                assertNull(viewModel.profile.value.userName)
                assertNull(viewModel.profile.value.email)
                assertNull(viewModel.profile.value.photoUrl)
            } finally {
                viewModelStore.clear()
                Dispatchers.resetMain()
            }
        }

    private suspend fun createAuthenticatedSession(dispatcher: CoroutineDispatcher): SessionManager {
        val sessionManager = SessionManager(FakeTokenStorage(), dispatcher)
        sessionManager.initialize()
        sessionManager.onLoginSucceeded(
            SessionToken(
                accessToken = "first-account-token",
                role = SessionRole.TEAM_MEMBER,
            ),
        )
        return sessionManager
    }

    private fun createViewModel(
        repository: FakeWorkerProfileRepository,
        sessionManager: SessionManager,
        viewModelStore: ViewModelStore,
    ): MenuDrawerViewModel {
        val viewModel =
            MenuDrawerViewModel(
                getWorkerProfileUseCase = GetWorkerProfileUseCase(repository),
                getWorkerProfilePhotoUseCase = GetWorkerProfilePhotoUseCase(repository),
                teamLogoutUseCase = TeamLogoutUseCase(FakeTeamAuthRepository()),
                sessionManager = sessionManager,
            )
        viewModelStore.put("menu-drawer", viewModel)
        return viewModel
    }

    private class FakeWorkerProfileRepository : WorkerProfileRepository {
        var profileRequestCount = 0
            private set
        var photoRequestCount = 0
            private set
        var profileRequest: suspend (Long?) -> ApiResult<WorkerProfile> = {
            ApiResult.Success(testProfile)
        }
        var photoRequest: suspend (Long) -> ApiResult<ProfilePhoto?> = {
            ApiResult.Success(null)
        }

        override suspend fun getWorkerProfile(expectedSessionGeneration: Long?): ApiResult<WorkerProfile> {
            profileRequestCount += 1
            return profileRequest(expectedSessionGeneration)
        }

        override suspend fun getProfilePhoto(expectedSessionGeneration: Long): ApiResult<ProfilePhoto?> {
            photoRequestCount += 1
            return photoRequest(expectedSessionGeneration)
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

        override suspend fun uploadProfilePhoto(
            photoUri: String,
            expectedSessionGeneration: Long,
        ): ApiResult<ProfilePhoto> = ApiResult.Failure(ApiError.Unknown)

        override suspend fun deleteProfilePhoto(expectedSessionGeneration: Long): ApiResult<Unit> =
            ApiResult.Failure(ApiError.Unknown)
    }

    private class FakeTeamAuthRepository : TeamAuthRepository {
        override suspend fun login(
            email: String,
            password: String,
        ): TeamLoginResult = error("login is unused in this test")

        override suspend fun logout() = Unit
    }

    private class FakeTokenStorage : TokenStorage {
        private var accessToken: String? = null

        override fun readAccessToken(): String? = accessToken

        override fun saveAccessToken(accessToken: String) {
            this.accessToken = accessToken
        }

        override fun readSessionRole(): SessionRole? = SessionRole.TEAM_MEMBER

        override fun clear() {
            accessToken = null
        }
    }

    private companion object {
        val testProfile =
            WorkerProfile(
                userId = "worker-1",
                name = "현장 작업자",
                email = "worker@example.test",
                companyName = "이음산업건설",
            )
    }
}
