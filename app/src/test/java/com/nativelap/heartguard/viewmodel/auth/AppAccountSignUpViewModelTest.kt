package com.nativelap.heartguard.viewmodel.auth

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionRole
import com.nativelap.heartguard.domain.auth.model.AppAccountRegistration
import com.nativelap.heartguard.domain.auth.model.AppEmailCode
import com.nativelap.heartguard.domain.auth.repository.AppAuthRepository
import com.nativelap.heartguard.domain.auth.usecase.LoginAppAccountUseCase
import com.nativelap.heartguard.domain.auth.usecase.RegisterAppAccountUseCase
import com.nativelap.heartguard.domain.auth.usecase.RequestAppEmailCodeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppAccountSignUpViewModelTest {
    @Test
    fun `이메일 코드 요청 후 가입하고 앱 계정으로 로그인한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val repository = FakeAppAuthRepository()
                val viewModel = createViewModel(repository)
                val password = testPassword()

                viewModel.requestEmailCode("worker@example.com")
                advanceUntilIdle()
                viewModel.signUp("worker@example.com", password, password, "123456")
                advanceUntilIdle()

                assertEquals(listOf("email-code", "register", "login"), repository.requestOrder)
                assertTrue(viewModel.uiState.value.isComplete)
                assertEquals(SessionRole.TEAM_MEMBER, repository.loginRole)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `다른 주소로 인증코드를 요청했으면 가입 요청을 보내지 않는다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val repository = FakeAppAuthRepository()
                val viewModel = createViewModel(repository)

                viewModel.requestEmailCode("worker@example.com")
                advanceUntilIdle()
                viewModel.signUp("other@example.com", testPassword(), testPassword(), "123456")
                advanceUntilIdle()

                assertEquals(listOf("email-code"), repository.requestOrder)
                assertEquals(AppAccountSignUpFailure.EMAIL_CODE_REQUIRED, viewModel.uiState.value.failure)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `인증코드 발송 오류는 계정 존재 여부를 노출하지 않는다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val repository =
                    FakeAppAuthRepository(
                        emailCodeResult =
                            ApiResult.Failure(
                                ApiError.Http(
                                    statusCode = 409,
                                    errorCode = "EMAIL_ALREADY_EXISTS",
                                ),
                            ),
                    )
                val viewModel = createViewModel(repository)

                viewModel.requestEmailCode("worker@example.com")
                advanceUntilIdle()

                assertEquals(AppAccountSignUpFailure.EMAIL_CODE_REQUEST, viewModel.uiState.value.failure)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `인증코드 요청 네트워크 오류를 화면 상태로 전달한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val repository =
                    FakeAppAuthRepository(
                        emailCodeResult = ApiResult.Failure(ApiError.Network),
                    )
                val viewModel = createViewModel(repository)

                viewModel.requestEmailCode("worker@example.com")
                advanceUntilIdle()

                assertEquals(AppAccountSignUpFailure.NETWORK, viewModel.uiState.value.failure)
                assertEquals(listOf("email-code"), repository.requestOrder)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `가입 서버 오류 코드를 사용자 상태로 변환하고 로그인하지 않는다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val registrationErrors =
                    listOf(
                        ApiError.Http(statusCode = 400, errorCode = "INVALID_VERIFICATION_CODE") to
                            AppAccountSignUpFailure.INVALID_VERIFICATION_CODE,
                        ApiError.Http(statusCode = 409, errorCode = "EMAIL_ALREADY_EXISTS") to
                            AppAccountSignUpFailure.EMAIL_ALREADY_EXISTS,
                        ApiError.Http(statusCode = 409, errorCode = "SILO_NOT_BOOTSTRAPPED") to
                            AppAccountSignUpFailure.ACCOUNT_NOT_READY,
                        ApiError.Http(statusCode = 429, errorCode = "RATE_LIMITED") to
                            AppAccountSignUpFailure.RATE_LIMITED,
                    )

                registrationErrors.forEach { (registrationError, expectedFailure) ->
                    val repository =
                        FakeAppAuthRepository(
                            registrationResult = ApiResult.Failure(registrationError),
                        )
                    val viewModel = createViewModel(repository)
                    val password = testPassword()

                    viewModel.requestEmailCode("worker@example.com")
                    advanceUntilIdle()
                    viewModel.signUp("worker@example.com", password, password, "123456")
                    advanceUntilIdle()

                    assertEquals(expectedFailure, viewModel.uiState.value.failure)
                    assertEquals(listOf("email-code", "register"), repository.requestOrder)
                }
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `인증코드 재전송은 60초 대기 후 허용된다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val repository = FakeAppAuthRepository()
                val viewModel = createViewModel(repository)

                viewModel.requestEmailCode("worker@example.com")
                runCurrent()
                viewModel.requestEmailCode("worker@example.com")
                runCurrent()

                assertEquals(60, viewModel.uiState.value.resendCooldownSeconds)
                assertEquals(listOf("email-code"), repository.requestOrder)

                advanceTimeBy(60_000)
                runCurrent()
                assertEquals(0, viewModel.uiState.value.resendCooldownSeconds)
                viewModel.requestEmailCode("worker@example.com")
                runCurrent()

                assertEquals(60, viewModel.uiState.value.resendCooldownSeconds)
                assertEquals(listOf("email-code", "email-code"), repository.requestOrder)
                advanceTimeBy(60_000)
                runCurrent()
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `연속 제출은 각 서버 요청을 한 번만 보낸다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val repository = FakeAppAuthRepository()
                val viewModel = createViewModel(repository)
                val password = testPassword()

                viewModel.requestEmailCode("worker@example.com")
                viewModel.requestEmailCode("worker@example.com")
                advanceUntilIdle()
                viewModel.signUp("worker@example.com", password, password, "123456")
                viewModel.signUp("worker@example.com", password, password, "123456")
                advanceUntilIdle()

                assertEquals(listOf("email-code", "register", "login"), repository.requestOrder)
                assertFalse(viewModel.uiState.value.isSubmitting)
            } finally {
                Dispatchers.resetMain()
            }
        }

    private fun testPassword(): String = CharArray(8) { 'x' }.concatToString()

    private fun createViewModel(repository: AppAuthRepository): AppAccountSignUpViewModel =
        AppAccountSignUpViewModel(
            requestAppEmailCodeUseCase = RequestAppEmailCodeUseCase(repository),
            registerAppAccountUseCase = RegisterAppAccountUseCase(repository),
            loginAppAccountUseCase = LoginAppAccountUseCase(repository),
        )

    private class FakeAppAuthRepository(
        private val emailCodeResult: ApiResult<AppEmailCode> =
            ApiResult.Success(AppEmailCode(expiresAt = "2026-10-23T12:10:00+09:00")),
        private val registrationResult: ApiResult<AppAccountRegistration> =
            ApiResult.Success(
                AppAccountRegistration(
                    userId = "user_01",
                    email = "worker@example.com",
                    createdAt = "2026-10-23T12:00:00+09:00",
                ),
            ),
    ) : AppAuthRepository {
        val requestOrder = mutableListOf<String>()
        var loginRole: SessionRole? = null

        override suspend fun requestEmailCode(email: String): ApiResult<AppEmailCode> {
            requestOrder += "email-code"
            return emailCodeResult
        }

        override suspend fun register(
            email: String,
            password: String,
            confirmPassword: String,
            verificationCode: String,
        ): ApiResult<AppAccountRegistration> {
            requestOrder += "register"
            return registrationResult
        }

        override suspend fun login(
            email: String,
            password: String,
        ): ApiResult<Unit> {
            requestOrder += "login"
            loginRole = SessionRole.TEAM_MEMBER
            return ApiResult.Success(Unit)
        }

        override suspend fun logout(): ApiResult<Unit> = ApiResult.Success(Unit)
    }
}
