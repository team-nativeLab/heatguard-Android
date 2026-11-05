package com.nativelap.heartguard.viewmodel.profile

import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.core.util.NoOpProfilePhotoMemoryCache
import com.nativelap.heartguard.core.util.RevisionedProfilePhotoMemoryCache
import com.nativelap.heartguard.domain.profile.model.PasswordChangeResult
import com.nativelap.heartguard.domain.profile.model.ProfilePhoto
import com.nativelap.heartguard.domain.profile.model.ProfileUpdateResult
import com.nativelap.heartguard.domain.profile.model.WorkerProfile
import com.nativelap.heartguard.domain.profile.repository.WorkerProfileRepository
import com.nativelap.heartguard.domain.profile.usecase.ClearWorkerProfilePhotoCacheUseCase
import com.nativelap.heartguard.domain.profile.usecase.DeleteWorkerProfilePhotoUseCase
import com.nativelap.heartguard.domain.profile.usecase.GetWorkerProfilePhotoUseCase
import com.nativelap.heartguard.domain.profile.usecase.GetWorkerProfileUseCase
import com.nativelap.heartguard.domain.profile.usecase.UpdateWorkerNameUseCase
import com.nativelap.heartguard.domain.profile.usecase.UploadWorkerProfilePhotoUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileEditViewModelParallelLoadTest {
    @Test
    fun `profile success is retained when parallel photo lookup fails`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val profileStarted = CompletableDeferred<Unit>()
            val photoStarted = CompletableDeferred<Unit>()
            val profileResult = CompletableDeferred<ApiResult<WorkerProfile>>()
            val photoResult = CompletableDeferred<ApiResult<ProfilePhoto?>>()
            val repository =
                FakeWorkerProfileRepository(
                    getProfile = {
                        profileStarted.complete(Unit)
                        profileResult.await()
                    },
                    getProfilePhoto = {
                        photoStarted.complete(Unit)
                        photoResult.await()
                    },
                )
            val sessionManager = SessionManager(TestTokenStorage(), dispatcher)
            sessionManager.onLoginSucceeded(SessionToken("profile-session-token"))
            val viewModel =
                ProfileEditViewModel(
                    getWorkerProfileUseCase = GetWorkerProfileUseCase(repository),
                    getWorkerProfilePhotoUseCase = GetWorkerProfilePhotoUseCase(repository),
                    updateWorkerNameUseCase = UpdateWorkerNameUseCase(repository),
                    uploadWorkerProfilePhotoUseCase = UploadWorkerProfilePhotoUseCase(repository),
                    deleteWorkerProfilePhotoUseCase = DeleteWorkerProfilePhotoUseCase(repository),
                    sessionManager = sessionManager,
                    profilePhotoMemoryCache = NoOpProfilePhotoMemoryCache,
                    clearWorkerProfilePhotoCacheUseCase = ClearWorkerProfilePhotoCacheUseCase(repository),
                )

            try {
                viewModel.loadProfile()
                runCurrent()

                assertTrue(profileStarted.isCompleted)
                assertTrue(photoStarted.isCompleted)

                profileResult.complete(
                    ApiResult.Success(
                        WorkerProfile(
                            userId = "worker-1",
                            name = "작업자",
                            email = "worker@example.invalid",
                            companyName = "현장 회사",
                            version = 3L,
                        ),
                    ),
                )
                photoResult.complete(ApiResult.Failure(ApiError.Network))
                runCurrent()

                val loadedProfile = viewModel.uiState.value.loadState as ProfileLoadState.Loaded
                assertEquals("작업자", loadedProfile.userName)
                assertEquals("worker@example.invalid", loadedProfile.email)
                assertEquals(ProfilePhotoSaveError.LOAD_FAILED, viewModel.uiState.value.profilePhotoSaveError)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `late photo response after photo revision changes is ignored`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val profileResult = CompletableDeferred<ApiResult<WorkerProfile>>()
            val photoResult = CompletableDeferred<ApiResult<ProfilePhoto?>>()
            val repository =
                FakeWorkerProfileRepository(
                    getProfile = { profileResult.await() },
                    getProfilePhoto = { photoResult.await() },
                )
            val sessionManager = SessionManager(TestTokenStorage(), dispatcher)
            sessionManager.onLoginSucceeded(SessionToken("profile-session-token"))
            val profilePhotoMemoryCache = RevisionedProfilePhotoMemoryCache()
            val viewModel =
                ProfileEditViewModel(
                    getWorkerProfileUseCase = GetWorkerProfileUseCase(repository),
                    getWorkerProfilePhotoUseCase = GetWorkerProfilePhotoUseCase(repository),
                    updateWorkerNameUseCase = UpdateWorkerNameUseCase(repository),
                    uploadWorkerProfilePhotoUseCase = UploadWorkerProfilePhotoUseCase(repository),
                    deleteWorkerProfilePhotoUseCase = DeleteWorkerProfilePhotoUseCase(repository),
                    sessionManager = sessionManager,
                    profilePhotoMemoryCache = profilePhotoMemoryCache,
                    clearWorkerProfilePhotoCacheUseCase = ClearWorkerProfilePhotoCacheUseCase(repository),
                )

            try {
                viewModel.loadProfile()
                runCurrent()
                profilePhotoMemoryCache.invalidateSessionPhoto(sessionManager.getSnapshot().generation)
                profileResult.complete(
                    ApiResult.Success(
                        WorkerProfile(
                            userId = "worker-1",
                            name = "작업자",
                            email = "worker@example.invalid",
                            companyName = "현장 회사",
                            version = 3L,
                        ),
                    ),
                )
                photoResult.complete(
                    ApiResult.Success(ProfilePhoto("https://images.example.test/old-photo", null)),
                )
                runCurrent()

                val loadedProfile = viewModel.uiState.value.loadState as ProfileLoadState.Loaded
                assertEquals("작업자", loadedProfile.userName)
                assertEquals("worker@example.invalid", loadedProfile.email)
                assertEquals(null, viewModel.uiState.value.profilePhotoUrl)
                assertEquals(null, viewModel.uiState.value.profilePhotoMemoryCacheKey)
                assertEquals(null, viewModel.uiState.value.profilePhotoSaveError)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `photo response is shown before profile lookup completes`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val profileResult = CompletableDeferred<ApiResult<WorkerProfile>>()
            val photoResult = CompletableDeferred<ApiResult<ProfilePhoto?>>()
            val repository =
                FakeWorkerProfileRepository(
                    getProfile = { profileResult.await() },
                    getProfilePhoto = { photoResult.await() },
                )
            val sessionManager = SessionManager(TestTokenStorage(), dispatcher)
            sessionManager.onLoginSucceeded(SessionToken("profile-session-token"))
            val viewModel =
                ProfileEditViewModel(
                    getWorkerProfileUseCase = GetWorkerProfileUseCase(repository),
                    getWorkerProfilePhotoUseCase = GetWorkerProfilePhotoUseCase(repository),
                    updateWorkerNameUseCase = UpdateWorkerNameUseCase(repository),
                    uploadWorkerProfilePhotoUseCase = UploadWorkerProfilePhotoUseCase(repository),
                    deleteWorkerProfilePhotoUseCase = DeleteWorkerProfilePhotoUseCase(repository),
                    sessionManager = sessionManager,
                    profilePhotoMemoryCache = NoOpProfilePhotoMemoryCache,
                    clearWorkerProfilePhotoCacheUseCase = ClearWorkerProfilePhotoCacheUseCase(repository),
                )

            try {
                viewModel.loadProfile()
                runCurrent()
                photoResult.complete(
                    ApiResult.Success(ProfilePhoto("https://images.example.test/current-photo", null)),
                )
                runCurrent()

                assertEquals(ProfileLoadState.Loading, viewModel.uiState.value.loadState)
                assertEquals("https://images.example.test/current-photo", viewModel.uiState.value.profilePhotoUrl)
                assertEquals(false, viewModel.uiState.value.canChangeProfilePhoto)
                assertEquals(false, viewModel.uiState.value.canSave)

                profileResult.complete(ApiResult.Failure(ApiError.Network))
                runCurrent()

                assertEquals(ProfileLoadState.Failed, viewModel.uiState.value.loadState)
                assertEquals("https://images.example.test/current-photo", viewModel.uiState.value.profilePhotoUrl)
                assertEquals(null, viewModel.uiState.value.profilePhotoSaveError)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `late photo failure after revision changes does not mark photo loading failed`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val profileResult = CompletableDeferred<ApiResult<WorkerProfile>>()
            val photoResult = CompletableDeferred<ApiResult<ProfilePhoto?>>()
            val repository =
                FakeWorkerProfileRepository(
                    getProfile = { profileResult.await() },
                    getProfilePhoto = { photoResult.await() },
                )
            val sessionManager = SessionManager(TestTokenStorage(), dispatcher)
            sessionManager.onLoginSucceeded(SessionToken("profile-session-token"))
            val profilePhotoMemoryCache = RevisionedProfilePhotoMemoryCache()
            val viewModel =
                ProfileEditViewModel(
                    getWorkerProfileUseCase = GetWorkerProfileUseCase(repository),
                    getWorkerProfilePhotoUseCase = GetWorkerProfilePhotoUseCase(repository),
                    updateWorkerNameUseCase = UpdateWorkerNameUseCase(repository),
                    uploadWorkerProfilePhotoUseCase = UploadWorkerProfilePhotoUseCase(repository),
                    deleteWorkerProfilePhotoUseCase = DeleteWorkerProfilePhotoUseCase(repository),
                    sessionManager = sessionManager,
                    profilePhotoMemoryCache = profilePhotoMemoryCache,
                    clearWorkerProfilePhotoCacheUseCase = ClearWorkerProfilePhotoCacheUseCase(repository),
                )

            try {
                viewModel.loadProfile()
                runCurrent()
                profilePhotoMemoryCache.invalidateSessionPhoto(sessionManager.getSnapshot().generation)
                photoResult.complete(ApiResult.Failure(ApiError.Network))
                runCurrent()
                profileResult.complete(
                    ApiResult.Success(
                        WorkerProfile(
                            userId = "worker-1",
                            name = "작업자",
                            email = "worker@example.invalid",
                            companyName = "현장 회사",
                            version = 3L,
                        ),
                    ),
                )
                runCurrent()

                assertEquals("작업자", (viewModel.uiState.value.loadState as ProfileLoadState.Loaded).userName)
                assertEquals(null, viewModel.uiState.value.profilePhotoSaveError)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    private class FakeWorkerProfileRepository(
        private val getProfile: suspend () -> ApiResult<WorkerProfile>,
        private val getProfilePhoto: suspend () -> ApiResult<ProfilePhoto?>,
    ) : WorkerProfileRepository {
        override suspend fun getWorkerProfile(expectedSessionGeneration: Long?) = getProfile()

        override suspend fun updateWorkerName(
            name: String,
            version: Long?,
            expectedSessionGeneration: Long?,
        ): ProfileUpdateResult = error("Profile update is not used in this test")

        override suspend fun changePassword(
            currentPassword: String,
            newPassword: String,
        ): PasswordChangeResult = error("Password change is not used in this test")

        override suspend fun getProfilePhoto(expectedSessionGeneration: Long) = getProfilePhoto()

        override suspend fun uploadProfilePhoto(
            photoUri: String,
            expectedSessionGeneration: Long,
        ): ApiResult<ProfilePhoto> = error("Photo upload is not used in this test")

        override suspend fun deleteProfilePhoto(expectedSessionGeneration: Long): ApiResult<Unit> =
            error("Photo deletion is not used in this test")
    }

    private class TestTokenStorage : TokenStorage {
        override fun readAccessToken(): String? = null

        override fun saveAccessToken(accessToken: String) = Unit

        override fun clear() = Unit
    }
}
