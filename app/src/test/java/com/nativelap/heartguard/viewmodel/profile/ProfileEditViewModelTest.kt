package com.nativelap.heartguard.viewmodel.profile

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.domain.profile.model.PasswordChangeResult
import com.nativelap.heartguard.domain.profile.model.WorkerProfile
import com.nativelap.heartguard.domain.profile.repository.WorkerProfileRepository
import com.nativelap.heartguard.domain.profile.usecase.GetWorkerProfileUseCase
import com.nativelap.heartguard.domain.profile.usecase.UpdateWorkerNameUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileEditViewModelTest {

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `조회에 성공하면 이름·이메일을 채우고 이름이 바뀌기 전에는 저장할 수 없다`() = runTest {
        val viewModel = createViewModel(FakeWorkerProfileRepository())

        viewModel.loadProfile()
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertEquals(
            ProfileLoadState.Loaded(userName = "홍길동", email = "worker01"),
            uiState.loadState,
        )
        assertEquals("홍길동", uiState.nameInput)
        assertFalse(uiState.canSave)

        viewModel.updateName("  ")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.updateName("김현장")
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `조회에 실패하면 Failed 상태가 된다`() = runTest {
        val viewModel = createViewModel(
            FakeWorkerProfileRepository(profileResult = ApiResult.Failure(ApiError.Network)),
        )

        viewModel.loadProfile()
        advanceUntilIdle()

        assertEquals(ProfileLoadState.Failed, viewModel.uiState.value.loadState)
    }

    @Test
    fun `이름 저장에 성공하면 앞뒤 공백을 제거해 보내고 Saved를 한 번 알린다`() = runTest {
        val repository = FakeWorkerProfileRepository()
        val viewModel = createViewModel(repository)
        viewModel.loadProfile()
        advanceUntilIdle()

        viewModel.updateName(" 김현장 ")
        viewModel.saveProfile()
        advanceUntilIdle()

        assertEquals(listOf("김현장"), repository.requestedNames)
        assertEquals(ProfileEditEffect.Saved, viewModel.effects.first())
        assertEquals("김현장", viewModel.uiState.value.nameInput)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `이름 저장에 실패하면 입력을 유지하고 오류를 표시한다`() = runTest {
        val repository = FakeWorkerProfileRepository(
            updateResult = ApiResult.Failure(ApiError.Http(statusCode = 400, errorCode = "VALIDATION_ERROR")),
        )
        val viewModel = createViewModel(repository)
        viewModel.loadProfile()
        advanceUntilIdle()

        viewModel.updateName("김현장")
        viewModel.saveProfile()
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertEquals("김현장", uiState.nameInput)
        assertTrue(uiState.hasSaveError)
        assertFalse(uiState.isSaving)
    }

    @Test
    fun `세션이 끝나면 이전 작업자 정보와 입력을 지운다`() = runTest {
        val sessionManager = createSessionManager()
        val viewModel = createViewModel(
            repository = FakeWorkerProfileRepository(),
            sessionManager = sessionManager,
        )
        viewModel.loadProfile()
        advanceUntilIdle()
        viewModel.updateName("김현장")

        sessionManager.expireSession()
        advanceUntilIdle()

        assertEquals(ProfileEditUiState(), viewModel.uiState.value)
    }

    private suspend fun TestScope.createSessionManager(): SessionManager {
        val sessionManager = SessionManager(
            tokenStorage = FakeTokenStorage(accessToken = "access-token"),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        sessionManager.initialize()
        return sessionManager
    }

    private suspend fun TestScope.createViewModel(
        repository: FakeWorkerProfileRepository,
        sessionManager: SessionManager? = null,
    ): ProfileEditViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return ProfileEditViewModel(
            getWorkerProfileUseCase = GetWorkerProfileUseCase(repository),
            updateWorkerNameUseCase = UpdateWorkerNameUseCase(repository),
            sessionManager = sessionManager ?: createSessionManager(),
        )
    }

    private class FakeWorkerProfileRepository(
        private val profileResult: ApiResult<WorkerProfile> = ApiResult.Success(
            WorkerProfile(
                userId = "usr_01",
                name = "홍길동",
                email = "worker01",
            ),
        ),
        private val updateResult: ApiResult<WorkerProfile>? = null,
    ) : WorkerProfileRepository {
        val requestedNames = mutableListOf<String>()

        override suspend fun getWorkerProfile(): ApiResult<WorkerProfile> = profileResult

        override suspend fun updateWorkerName(name: String): ApiResult<WorkerProfile> {
            requestedNames += name
            return updateResult ?: ApiResult.Success(
                WorkerProfile(
                    userId = "usr_01",
                    name = name,
                    email = "worker01",
                ),
            )
        }

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
}
