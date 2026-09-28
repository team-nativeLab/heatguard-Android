package com.nativelap.heartguard.viewmodel.password

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.domain.profile.model.PasswordChangeResult
import com.nativelap.heartguard.domain.profile.model.WorkerProfile
import com.nativelap.heartguard.domain.profile.repository.WorkerProfileRepository
import com.nativelap.heartguard.domain.profile.usecase.ChangeWorkerPasswordUseCase
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
class PasswordChangeViewModelTest {

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `규칙과 확인 일치를 모두 만족해야 변경할 수 있다`() = runTest {
        val viewModel = createViewModel(FakeWorkerProfileRepository(PasswordChangeResult.Success))

        viewModel.updateCurrentPassword("current1")
        viewModel.updateNewPassword("abcdefgh")
        assertFalse(viewModel.uiState.value.isNewPasswordValid)

        viewModel.updateNewPassword("abcd1234")
        viewModel.updateConfirmPassword("abcd1235")
        assertTrue(viewModel.uiState.value.isConfirmMismatched)
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.updateConfirmPassword("abcd1234")
        assertTrue(viewModel.uiState.value.isConfirmMatched)
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `변경에 성공하면 입력을 비우고 Changed를 알린다`() = runTest {
        val repository = FakeWorkerProfileRepository(PasswordChangeResult.Success)
        val viewModel = createViewModel(repository)
        fillValidInput(viewModel)

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(listOf("current1" to "abcd1234"), repository.requestedChanges)
        assertEquals(PasswordChangeEffect.Changed, viewModel.effects.first())
        assertEquals(PasswordChangeUiState(), viewModel.uiState.value)
    }

    @Test
    fun `현재 비밀번호가 틀리면 입력을 유지하고 오류를 표시한다`() = runTest {
        val viewModel = createViewModel(FakeWorkerProfileRepository(PasswordChangeResult.InvalidCurrentPassword))
        fillValidInput(viewModel)

        viewModel.submit()
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertEquals(PasswordChangeError.INVALID_CURRENT_PASSWORD, uiState.submitError)
        assertEquals("current1", uiState.currentPassword)
        assertFalse(uiState.isSubmitting)
    }

    @Test
    fun `세션이 끝나면 입력한 비밀번호를 지운다`() = runTest {
        val sessionManager = createSessionManager()
        val viewModel = createViewModel(
            repository = FakeWorkerProfileRepository(PasswordChangeResult.Success),
            sessionManager = sessionManager,
        )
        fillValidInput(viewModel)

        sessionManager.expireSession()
        advanceUntilIdle()

        assertEquals(PasswordChangeUiState(), viewModel.uiState.value)
    }

    @Test
    fun `영문과 숫자를 모두 포함한 8자 이상만 유효하다`() {
        assertFalse(ChangeWorkerPasswordUseCase.isValidNewPassword("abc1234"))
        assertFalse(ChangeWorkerPasswordUseCase.isValidNewPassword("12345678"))
        assertFalse(ChangeWorkerPasswordUseCase.isValidNewPassword("abcdefgh"))
        assertTrue(ChangeWorkerPasswordUseCase.isValidNewPassword("abcd1234"))
    }

    private fun fillValidInput(viewModel: PasswordChangeViewModel) {
        viewModel.updateCurrentPassword("current1")
        viewModel.updateNewPassword("abcd1234")
        viewModel.updateConfirmPassword("abcd1234")
    }

    private suspend fun TestScope.createSessionManager(): SessionManager {
        val sessionManager = SessionManager(
            tokenStorage = FakeTokenStorage(),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        sessionManager.initialize()
        return sessionManager
    }

    private suspend fun TestScope.createViewModel(
        repository: FakeWorkerProfileRepository,
        sessionManager: SessionManager? = null,
    ): PasswordChangeViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return PasswordChangeViewModel(
            changeWorkerPasswordUseCase = ChangeWorkerPasswordUseCase(repository),
            sessionManager = sessionManager ?: createSessionManager(),
        )
    }

    private class FakeWorkerProfileRepository(
        private val passwordChangeResult: PasswordChangeResult,
    ) : WorkerProfileRepository {
        val requestedChanges = mutableListOf<Pair<String, String>>()

        override suspend fun getWorkerProfile(): ApiResult<WorkerProfile> = error("not used")

        override suspend fun updateWorkerName(name: String): ApiResult<WorkerProfile> = error("not used")

        override suspend fun changePassword(
            currentPassword: String,
            newPassword: String,
        ): PasswordChangeResult {
            requestedChanges += currentPassword to newPassword
            return passwordChangeResult
        }
    }

    private class FakeTokenStorage : TokenStorage {
        private var accessToken: String? = "access-token"

        override fun readAccessToken(): String? = accessToken

        override fun saveAccessToken(accessToken: String) {
            this.accessToken = accessToken
        }

        override fun clear() {
            accessToken = null
        }
    }
}
