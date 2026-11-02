package com.nativelap.heartguard.viewmodel.emergency

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallState
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallStatus
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallUpdateResult
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallUpdateStatus
import com.nativelap.heartguard.domain.emergency.repository.EmergencyCallRepository
import com.nativelap.heartguard.domain.emergency.usecase.GetCurrentEmergencyCallUseCase
import com.nativelap.heartguard.domain.emergency.usecase.ObserveEmergencyCallStatusUseCase
import com.nativelap.heartguard.domain.emergency.usecase.RegisterEmergencyCallUseCase
import com.nativelap.heartguard.domain.emergency.usecase.UpdateEmergencyCallStatusUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EmergencyViewModelTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `failed registration can be dismissed once the server confirms there is no call`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val repository = FakeEmergencyCallRepository()
            repository.registerResults.add(
                ApiResult.Failure(ApiError.Http(statusCode = 500, errorCode = null, errorMessage = null)),
            )
            val viewModel = createViewModel(repository, dispatcher)
            try {
                val effects = mutableListOf<EmergencyEffect>()
                backgroundScope.launch { viewModel.effects.collect { effects.add(it) } }

                viewModel.startEmergencyCall()
                runCurrent()
                assertTrue(viewModel.uiState.value.hasRegistrationFailed)
                assertTrue(viewModel.uiState.value.registrationMayExist)

                viewModel.requestDismissDuringRegistration()
                runCurrent()

                assertEquals(1, effects.count { it is EmergencyEffect.RegistrationDismissed })
            } finally {
                viewModel.reset()
            }
        }

    @Test
    fun `failed registration stays unresolved while the server cannot confirm the call`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val repository = FakeEmergencyCallRepository()
            repository.registerResults.add(
                ApiResult.Failure(ApiError.Http(statusCode = 500, errorCode = null, errorMessage = null)),
            )
            repository.currentResults.add(ApiResult.Failure(ApiError.Network))
            val viewModel = createViewModel(repository, dispatcher)
            try {
                val effects = mutableListOf<EmergencyEffect>()
                backgroundScope.launch { viewModel.effects.collect { effects.add(it) } }

                viewModel.startEmergencyCall()
                runCurrent()
                viewModel.requestDismissDuringRegistration()
                runCurrent()

                assertTrue(viewModel.uiState.value.registrationMayExist)
                assertEquals(0, effects.count { it is EmergencyEffect.RegistrationDismissed })
            } finally {
                viewModel.reset()
            }
        }

    @Test
    fun `failed cancel is resent by polling while the server still reports the call in progress`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val repository = FakeEmergencyCallRepository()
            repository.registerResults.add(
                ApiResult.Success(EmergencyCallStatus("call-1", EmergencyCallState.ACTIVE, null)),
            )
            repository.currentDefault =
                ApiResult.Success(EmergencyCallStatus("call-1", EmergencyCallState.ACTIVE, null))
            repository.updateResults.add(EmergencyCallUpdateResult.Failure)
            repository.updateResults.add(
                EmergencyCallUpdateResult.Updated(EmergencyCallStatus("call-1", EmergencyCallState.CANCELLED, null)),
            )
            val viewModel = createViewModel(repository, dispatcher)
            try {
                viewModel.startEmergencyCall()
                runCurrent()
                viewModel.updateCallStatus(EmergencyCallUpdateStatus.CANCELLED)
                runCurrent()

                assertEquals("call-1", viewModel.uiState.value.callId)
                assertTrue(viewModel.uiState.value.statusUpdateFailed)
                assertFalse(viewModel.uiState.value.isUpdatingStatus)

                advanceTimeBy(POLL_INTERVAL_MS + 1)
                runCurrent()

                assertNull(viewModel.uiState.value.callId)
                assertEquals(2, repository.updateRequests.size)
            } finally {
                viewModel.reset()
            }
        }

    @Test
    fun `a cancel response that is not closed keeps the request pending`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val repository = FakeEmergencyCallRepository()
            repository.registerResults.add(
                ApiResult.Success(EmergencyCallStatus("call-1", EmergencyCallState.ACTIVE, null)),
            )
            repository.currentDefault =
                ApiResult.Success(EmergencyCallStatus("call-1", EmergencyCallState.ACTIVE, null))
            repository.updateResults.add(
                EmergencyCallUpdateResult.Updated(EmergencyCallStatus("call-1", EmergencyCallState.ACTIVE, null)),
            )
            repository.updateResults.add(
                EmergencyCallUpdateResult.Updated(EmergencyCallStatus("call-1", EmergencyCallState.CANCELLED, null)),
            )
            val viewModel = createViewModel(repository, dispatcher)
            try {
                viewModel.startEmergencyCall()
                runCurrent()
                viewModel.updateCallStatus(EmergencyCallUpdateStatus.CANCELLED)
                runCurrent()

                assertEquals("call-1", viewModel.uiState.value.callId)
                assertFalse(viewModel.uiState.value.isUpdatingStatus)

                advanceTimeBy(POLL_INTERVAL_MS + 1)
                runCurrent()

                assertNull(viewModel.uiState.value.callId)
            } finally {
                viewModel.reset()
            }
        }

    @Test
    fun `automatic resend stops after the limit and waits for the user`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val repository = FakeEmergencyCallRepository()
            repository.registerResults.add(
                ApiResult.Success(EmergencyCallStatus("call-1", EmergencyCallState.ACTIVE, null)),
            )
            repository.currentDefault =
                ApiResult.Success(EmergencyCallStatus("call-1", EmergencyCallState.ACTIVE, null))
            repeat(MAX_TOTAL_REQUESTS + 5) {
                repository.updateResults.add(EmergencyCallUpdateResult.Failure)
            }
            val viewModel = createViewModel(repository, dispatcher)
            try {
                viewModel.startEmergencyCall()
                runCurrent()
                viewModel.updateCallStatus(EmergencyCallUpdateStatus.CANCELLED)
                runCurrent()
                repeat(MAX_TOTAL_REQUESTS + 2) {
                    advanceTimeBy(POLL_INTERVAL_MS + 1)
                    runCurrent()
                }

                // 최초 요청 1회와 자동 재시도 상한 10회다.
                assertEquals(MAX_TOTAL_REQUESTS, repository.updateRequests.size)
                assertEquals("call-1", viewModel.uiState.value.callId)
            } finally {
                viewModel.reset()
            }
        }

    @Test
    fun `pending cancel is not resent as completed after the admin acknowledges the call`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val repository = FakeEmergencyCallRepository()
            repository.registerResults.add(
                ApiResult.Success(EmergencyCallStatus("call-1", EmergencyCallState.ACTIVE, null)),
            )
            repository.currentDefault =
                ApiResult.Success(EmergencyCallStatus("call-1", EmergencyCallState.ACTIVE, null))
            repository.updateResults.add(EmergencyCallUpdateResult.Failure)
            val viewModel = createViewModel(repository, dispatcher)
            try {
                viewModel.startEmergencyCall()
                runCurrent()
                viewModel.updateCallStatus(EmergencyCallUpdateStatus.CANCELLED)
                runCurrent()

                repository.currentDefault =
                    ApiResult.Success(EmergencyCallStatus("call-1", EmergencyCallState.ACKNOWLEDGED, null))
                advanceTimeBy(POLL_INTERVAL_MS + 1)
                runCurrent()

                assertEquals(listOf(EmergencyCallUpdateStatus.CANCELLED), repository.updateRequests)
                assertEquals("call-1", viewModel.uiState.value.callId)
            } finally {
                viewModel.reset()
            }
        }

    @Test
    fun `pending cancel is dropped when the transition is rejected and the call has moved on`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val repository = FakeEmergencyCallRepository()
            repository.registerResults.add(
                ApiResult.Success(EmergencyCallStatus("call-1", EmergencyCallState.ACTIVE, null)),
            )
            repository.currentDefault =
                ApiResult.Success(EmergencyCallStatus("call-1", EmergencyCallState.ACTIVE, null))
            repository.updateResults.add(EmergencyCallUpdateResult.InvalidTransition)
            val viewModel = createViewModel(repository, dispatcher)
            try {
                viewModel.startEmergencyCall()
                runCurrent()
                repository.currentDefault =
                    ApiResult.Success(EmergencyCallStatus("call-1", EmergencyCallState.ACKNOWLEDGED, null))
                viewModel.updateCallStatus(EmergencyCallUpdateStatus.CANCELLED)
                runCurrent()
                advanceTimeBy(POLL_INTERVAL_MS + 1)
                runCurrent()

                assertEquals(listOf(EmergencyCallUpdateStatus.CANCELLED), repository.updateRequests)
                assertEquals(EmergencyCallState.ACKNOWLEDGED, viewModel.uiState.value.callState)
            } finally {
                viewModel.reset()
            }
        }

    @Test
    fun `registration with an unknown status and a call id does not confirm that no call exists`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val repository = FakeEmergencyCallRepository()
            repository.registerResults.add(
                ApiResult.Success(EmergencyCallStatus("call-1", EmergencyCallState.UNKNOWN, null)),
            )
            val viewModel = createViewModel(repository, dispatcher)
            try {
                viewModel.startEmergencyCall()
                runCurrent()

                assertTrue(viewModel.uiState.value.hasRegistrationFailed)
                assertTrue(viewModel.uiState.value.registrationMayExist)
            } finally {
                viewModel.reset()
            }
        }

    @Test
    fun `a cancel without a response fails after the timeout and is resent once polling confirms the call`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val repository = FakeEmergencyCallRepository()
            repository.registerResults.add(
                ApiResult.Success(EmergencyCallStatus("call-1", EmergencyCallState.ACTIVE, null)),
            )
            repository.currentDefault =
                ApiResult.Success(EmergencyCallStatus("call-1", EmergencyCallState.ACTIVE, null))
            repository.updateResults.add(
                EmergencyCallUpdateResult.Updated(EmergencyCallStatus("call-1", EmergencyCallState.CANCELLED, null)),
            )
            repository.updateBlocker = { awaitCancellation() }
            val viewModel = createViewModel(repository, dispatcher)
            try {
                viewModel.startEmergencyCall()
                runCurrent()
                viewModel.updateCallStatus(EmergencyCallUpdateStatus.CANCELLED)
                runCurrent()
                assertTrue(viewModel.uiState.value.isUpdatingStatus)

                advanceTimeBy(REQUEST_TIMEOUT_MS + 1)
                runCurrent()
                assertFalse(viewModel.uiState.value.isUpdatingStatus)
                assertTrue(viewModel.uiState.value.statusUpdateFailed)
                assertEquals("call-1", viewModel.uiState.value.callId)

                repository.updateBlocker = null
                advanceTimeBy(POLL_INTERVAL_MS + 1)
                runCurrent()

                assertNull(viewModel.uiState.value.callId)
                assertEquals(2, repository.updateRequests.size)
            } finally {
                viewModel.reset()
            }
        }

    @Test
    fun `a registration without a response fails after the timeout`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val repository = FakeEmergencyCallRepository()
            repository.registerBlocker = { awaitCancellation() }
            val viewModel = createViewModel(repository, dispatcher)
            try {
                viewModel.startEmergencyCall()
                runCurrent()
                assertTrue(viewModel.uiState.value.isRegistering)

                advanceTimeBy(REQUEST_TIMEOUT_MS + 1)
                runCurrent()

                assertFalse(viewModel.uiState.value.isRegistering)
                assertTrue(viewModel.uiState.value.hasRegistrationFailed)
                assertTrue(viewModel.uiState.value.registrationMayExist)
            } finally {
                viewModel.reset()
            }
        }

    private suspend fun createViewModel(
        repository: EmergencyCallRepository,
        dispatcher: CoroutineDispatcher,
    ): EmergencyViewModel {
        val sessionManager = SessionManager(TestTokenStorage(), dispatcher)
        sessionManager.onLoginSucceeded(SessionToken("session-token"))
        return EmergencyViewModel(
            registerEmergencyCallUseCase = RegisterEmergencyCallUseCase(repository),
            observeEmergencyCallStatusUseCase = ObserveEmergencyCallStatusUseCase(repository),
            updateEmergencyCallStatusUseCase = UpdateEmergencyCallStatusUseCase(repository),
            getCurrentEmergencyCallUseCase = GetCurrentEmergencyCallUseCase(repository),
            sessionManager = sessionManager,
            clock = Clock.fixed(Instant.parse("2026-11-02T00:00:00Z"), ZoneId.of("Asia/Seoul")),
        )
    }

    private class FakeEmergencyCallRepository : EmergencyCallRepository {
        val registerResults = ArrayDeque<ApiResult<EmergencyCallStatus>>()
        val currentResults = ArrayDeque<ApiResult<EmergencyCallStatus>>()
        var currentDefault: ApiResult<EmergencyCallStatus> =
            ApiResult.Success(
                EmergencyCallStatus(callId = null, state = EmergencyCallState.NONE, acknowledgedAt = null),
            )
        val updateResults = ArrayDeque<EmergencyCallUpdateResult>()
        val updateRequests = mutableListOf<EmergencyCallUpdateStatus>()

        // 값이 있으면 응답 대신 이 블록을 실행한다. 서버가 응답하지 않는 상황을 흉내 낸다.
        var registerBlocker: (suspend () -> Unit)? = null
        var updateBlocker: (suspend () -> Unit)? = null

        override suspend fun registerEmergencyCall(
            idempotencyKey: String,
            clientOccurredAt: Instant,
            message: String?,
        ): ApiResult<EmergencyCallStatus> {
            registerBlocker?.invoke()
            return registerResults.removeFirst()
        }

        override suspend fun getCurrentEmergencyCallStatus(): ApiResult<EmergencyCallStatus> =
            currentResults.removeFirstOrNull() ?: currentDefault

        override suspend fun updateEmergencyCallStatus(
            callId: String,
            status: EmergencyCallUpdateStatus,
        ): EmergencyCallUpdateResult {
            updateRequests.add(status)
            updateBlocker?.invoke()
            return updateResults.removeFirst()
        }
    }

    private class TestTokenStorage : TokenStorage {
        override fun readAccessToken(): String? = null

        override fun saveAccessToken(accessToken: String) = Unit

        override fun clear() = Unit
    }

    private companion object {
        const val POLL_INTERVAL_MS = 3_000L

        // EmergencyViewModel의 EMERGENCY_REQUEST_TIMEOUT_MS와 같은 값이다.
        const val REQUEST_TIMEOUT_MS = 8_000L

        // 최초 요청 1회와 자동 재시도 상한(EmergencyViewModel의 MAX_AUTOMATIC_STATUS_UPDATE_ATTEMPTS = 10)을 합한 값이다.
        const val MAX_TOTAL_REQUESTS = 11
    }
}
