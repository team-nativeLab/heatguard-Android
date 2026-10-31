package com.nativelap.heartguard.viewmodel.profile

import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.domain.profile.model.PasswordChangeResult
import com.nativelap.heartguard.domain.profile.model.ProfilePhoto
import com.nativelap.heartguard.domain.profile.model.ProfileUpdateResult
import com.nativelap.heartguard.domain.profile.model.WorkerProfile
import com.nativelap.heartguard.domain.profile.repository.WorkerProfileRepository
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
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileEditViewModelParallelLoadTest {
    @Test
    fun `profile success is retained when parallel photo lookup fails`() = runTest {
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
