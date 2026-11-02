package com.nativelap.heartguard.viewmodel.profile

import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileEditViewModelSavingTest {
    @Test
    fun `profile photo can be changed only when no save is in progress`() {
        val idleState = ProfileEditUiState(loadState = ProfileLoadState.Loaded(userName = "작업자", email = null))
        val savingState = idleState.copy(isSaving = true)

        assertTrue(idleState.canChangeProfilePhoto)
        assertFalse(savingState.canChangeProfilePhoto)
    }

    @Test
    fun `default photo request while saving does not mark the server photo for deletion`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val nameUpdate = CompletableDeferred<ProfileUpdateResult>()
            val repository =
                FakeWorkerProfileRepository(
                    photo = ProfilePhoto(photoUrl = "https://example.invalid/photo.png", expiresAt = null),
                    updateName = { _, _ -> nameUpdate.await() },
                )
            val viewModel = newViewModel(repository, dispatcher)

            try {
                viewModel.loadProfile()
                runCurrent()
                viewModel.updateName("새 이름")
                viewModel.saveProfile()
                runCurrent()

                viewModel.requestProfilePhotoDeletion()

                assertTrue(viewModel.uiState.value.isSaving)
                assertFalse(viewModel.uiState.value.shouldDeleteProfilePhoto)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `clearing the photo from a leave path while saving keeps the in-flight deletion`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val photoDeletion = CompletableDeferred<ApiResult<Unit>>()
            val repository =
                FakeWorkerProfileRepository(
                    photo = ProfilePhoto(photoUrl = "https://example.invalid/photo.png", expiresAt = null),
                    deletePhoto = { photoDeletion.await() },
                )
            val viewModel = newViewModel(repository, dispatcher)

            try {
                viewModel.loadProfile()
                runCurrent()
                viewModel.requestProfilePhotoDeletion()
                viewModel.saveProfile()
                runCurrent()

                viewModel.clearProfilePhoto()

                assertTrue(viewModel.uiState.value.isSaving)
                assertTrue(viewModel.uiState.value.shouldDeleteProfilePhoto)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `saved effect is delivered to a screen that is collecting`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val repository =
                FakeWorkerProfileRepository(
                    updateName = { name, _ ->
                        ProfileUpdateResult.Success(
                            WorkerProfile(
                                userId = "worker-1",
                                name = name,
                                email = "worker@example.invalid",
                                version = 4L,
                            ),
                        )
                    },
                )
            val viewModel = newViewModel(repository, dispatcher)
            val receivedEffects = mutableListOf<ProfileEditEffect>()

            try {
                backgroundScope.launch {
                    viewModel.effects.collect { effect -> receivedEffects.add(effect) }
                }
                runCurrent()
                viewModel.loadProfile()
                runCurrent()
                viewModel.updateName("새 이름")
                viewModel.saveProfile()
                runCurrent()

                assertEquals(listOf<ProfileEditEffect>(ProfileEditEffect.Saved), receivedEffects)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `saved effect finished without a collector is not delivered to a later screen`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val repository =
                FakeWorkerProfileRepository(
                    updateName = { name, _ ->
                        ProfileUpdateResult.Success(
                            WorkerProfile(
                                userId = "worker-1",
                                name = name,
                                email = "worker@example.invalid",
                                version = 4L,
                            ),
                        )
                    },
                )
            val viewModel = newViewModel(repository, dispatcher)
            val receivedEffects = mutableListOf<ProfileEditEffect>()

            try {
                viewModel.loadProfile()
                runCurrent()
                viewModel.updateName("새 이름")
                viewModel.saveProfile()
                runCurrent()

                backgroundScope.launch {
                    viewModel.effects.collect { effect -> receivedEffects.add(effect) }
                }
                runCurrent()

                assertTrue(receivedEffects.isEmpty())
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    private suspend fun newViewModel(
        repository: WorkerProfileRepository,
        dispatcher: TestDispatcher,
    ): ProfileEditViewModel {
        val sessionManager = SessionManager(TestTokenStorage(), dispatcher)
        sessionManager.onLoginSucceeded(SessionToken("profile-saving-session-token"))
        return ProfileEditViewModel(
            getWorkerProfileUseCase = GetWorkerProfileUseCase(repository),
            getWorkerProfilePhotoUseCase = GetWorkerProfilePhotoUseCase(repository),
            updateWorkerNameUseCase = UpdateWorkerNameUseCase(repository),
            uploadWorkerProfilePhotoUseCase = UploadWorkerProfilePhotoUseCase(repository),
            deleteWorkerProfilePhotoUseCase = DeleteWorkerProfilePhotoUseCase(repository),
            sessionManager = sessionManager,
        )
    }

    private class FakeWorkerProfileRepository(
        private val photo: ProfilePhoto? = null,
        private val updateName: suspend (name: String, version: Long?) -> ProfileUpdateResult = { _, _ ->
            error("Profile update is not used in this test")
        },
        private val deletePhoto: suspend () -> ApiResult<Unit> = {
            error("Photo deletion is not used in this test")
        },
    ) : WorkerProfileRepository {
        override suspend fun getWorkerProfile(expectedSessionGeneration: Long?): ApiResult<WorkerProfile> =
            ApiResult.Success(
                WorkerProfile(
                    userId = "worker-1",
                    name = "작업자",
                    email = "worker@example.invalid",
                    companyName = "현장 회사",
                    version = 3L,
                ),
            )

        override suspend fun updateWorkerName(
            name: String,
            version: Long?,
            expectedSessionGeneration: Long?,
        ): ProfileUpdateResult = updateName(name, version)

        override suspend fun changePassword(
            currentPassword: String,
            newPassword: String,
        ): PasswordChangeResult = error("Password change is not used in this test")

        override suspend fun getProfilePhoto(expectedSessionGeneration: Long): ApiResult<ProfilePhoto?> =
            ApiResult.Success(photo)

        override suspend fun uploadProfilePhoto(
            photoUri: String,
            expectedSessionGeneration: Long,
        ): ApiResult<ProfilePhoto> = error("Photo upload is not used in this test")

        override suspend fun deleteProfilePhoto(expectedSessionGeneration: Long): ApiResult<Unit> = deletePhoto()
    }

    private class TestTokenStorage : TokenStorage {
        override fun readAccessToken(): String? = null

        override fun saveAccessToken(accessToken: String) = Unit

        override fun clear() = Unit
    }
}
