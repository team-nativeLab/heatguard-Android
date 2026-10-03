package com.nativelap.heartguard.viewmodel.emergency

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.core.session.createTestSessionManager
import com.nativelap.heartguard.core.session.createUnauthenticatedTestSessionManager
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallState
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallStatus
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallUpdateResult
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallUpdateStatus
import com.nativelap.heartguard.domain.emergency.repository.EmergencyCallRepository
import com.nativelap.heartguard.domain.emergency.usecase.GetCurrentEmergencyCallUseCase
import com.nativelap.heartguard.domain.emergency.usecase.ObserveEmergencyCallStatusUseCase
import com.nativelap.heartguard.domain.emergency.usecase.RegisterEmergencyCallUseCase
import com.nativelap.heartguard.domain.emergency.usecase.UpdateEmergencyCallStatusUseCase
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EmergencyViewModelTest {
    @Test
    fun networkFailureAfterConflictKeepsCallAndPolling() = runTest {
        assertConflictRefreshFailure(ApiError.Network)
    }

    @Test
    fun rateLimitAfterConflictKeepsCallAndPolling() = runTest {
        assertConflictRefreshFailure(ApiError.Http(429, null))
    }

    @Test
    fun serverFailureAfterConflictKeepsCallAndPolling() = runTest {
        assertConflictRefreshFailure(ApiError.Http(500, null))
    }

    @Test
    fun pollingNoneClosesActiveCallOnceAndStopsPolling() = runTest {
        val repository = FakeEmergencyCallRepository()
        val viewModel = createViewModel(repository)
        val receivedEffects = mutableListOf<EmergencyEffect>()
        backgroundScope.launch { viewModel.effects.collect { receivedEffects += it } }
        viewModel.startEmergencyCall()
        runCurrent()
        repository.currentCall = EmergencyCallStatus(null, EmergencyCallState.NONE, null)
        advanceTimeBy(POLLING_INTERVAL_MS + 1)
        runCurrent()
        assertNull(viewModel.uiState.value.callId)
        assertEquals(1, receivedEffects.count { it is EmergencyEffect.CallClosed })
        val completedRequestCount = repository.currentRequestCount
        advanceTimeBy(POLLING_INTERVAL_MS * 3)
        runCurrent()
        assertEquals(completedRequestCount, repository.currentRequestCount)
        assertEquals(1, receivedEffects.count { it is EmergencyEffect.CallClosed })
        viewModel.reset()
    }

    @Test
    fun accountSwitchClearsActiveCallAndStopsPolling() = runTest {
        val repository = FakeEmergencyCallRepository()
        val sessionManager = createTestSessionManager(StandardTestDispatcher(testScheduler))
        sessionManager.onLoginSucceeded(SessionToken("first-account-token"))
        val viewModel = createViewModel(repository, sessionManager)
        runCurrent()
        viewModel.startEmergencyCall()
        runCurrent()
        assertEquals("call_new", viewModel.uiState.value.callId)

        sessionManager.expireSession()
        sessionManager.onLoginSucceeded(SessionToken("second-account-token"))
        runCurrent()
        val requestCountAfterSwitch = repository.currentRequestCount
        advanceTimeBy(POLLING_INTERVAL_MS * 3)
        runCurrent()

        assertNull(viewModel.uiState.value.callId)
        assertEquals(requestCountAfterSwitch, repository.currentRequestCount)
    }

    @Test
    fun unknownStateAfterConflictKeepsSameCallOpen() = runTest {
        val repository = FakeEmergencyCallRepository()
        val viewModel = createViewModel(repository)
        val receivedEffects = mutableListOf<EmergencyEffect>()
        backgroundScope.launch { viewModel.effects.collect { receivedEffects += it } }
        viewModel.startEmergencyCall()
        runCurrent()
        repository.currentCall = callStatus("call_new", EmergencyCallState.UNKNOWN)
        repository.updateResult = EmergencyCallUpdateResult.InvalidTransition

        viewModel.updateCallStatus(EmergencyCallUpdateStatus.CANCELLED)
        runCurrent()

        assertEquals("call_new", viewModel.uiState.value.callId)
        assertEquals(true, viewModel.uiState.value.statusUpdateFailed)
        assertEquals(0, receivedEffects.count { it is EmergencyEffect.CallClosed })
        viewModel.reset()
    }

    @Test
    fun failedResumeCanBeRetriedOnNextHomeRefresh() = runTest {
        val repository = FakeEmergencyCallRepository()
        val viewModel = createViewModel(repository)
        repository.currentCallResult = ApiResult.Failure(ApiError.Network)
        viewModel.resumeActiveCall()
        runCurrent()
        assertNull(viewModel.uiState.value.callId)

        repository.currentCallResult = null
        viewModel.resumeActiveCall()
        runCurrent()

        assertEquals("call_new", viewModel.uiState.value.callId)
        viewModel.reset()
    }

    @Test
    fun lateCancellationResultCannotCloseReenteredCall() = runTest {
        val repository = FakeEmergencyCallRepository()
        val viewModel = createViewModel(repository)
        viewModel.startEmergencyCall()
        runCurrent()
        val deferredUpdate = CompletableDeferred<EmergencyCallUpdateResult>()
        repository.delayedUpdate = deferredUpdate
        viewModel.updateCallStatus(EmergencyCallUpdateStatus.CANCELLED)
        runCurrent()
        repository.currentCall = callStatus("call_new", EmergencyCallState.COMPLETED)
        advanceTimeBy(POLLING_INTERVAL_MS + 1)
        runCurrent()
        assertNull(viewModel.uiState.value.callId)
        repository.currentCall = callStatus("call_new", EmergencyCallState.ACTIVE)
        viewModel.startEmergencyCall()
        runCurrent()
        deferredUpdate.complete(EmergencyCallUpdateResult.AlreadyClosed)
        runCurrent()
        assertEquals("call_new", viewModel.uiState.value.callId)
        assertFalse(viewModel.uiState.value.isUpdatingStatus)
        viewModel.reset()
    }

    @Test
    fun queuedClosedEffectIsIgnoredAfterNewCallIntentTwentyTimes() = runTest {
        val repository = FakeEmergencyCallRepository()
        val viewModel = createViewModel(repository)
        repeat(20) {
            viewModel.startEmergencyCall()
            runCurrent()
            viewModel.effects.first()
            viewModel.updateCallStatus(EmergencyCallUpdateStatus.CANCELLED)
            runCurrent()
            val oldClosedEffect = viewModel.effects.first()
            assertTrue(viewModel.isCurrentEffect(oldClosedEffect))
            viewModel.startEmergencyCall()
            runCurrent()
            assertFalse(viewModel.isCurrentEffect(oldClosedEffect))
            assertEquals("call_new", viewModel.uiState.value.callId)
            viewModel.effects.first()
            viewModel.updateCallStatus(EmergencyCallUpdateStatus.CANCELLED)
            runCurrent()
            viewModel.effects.first()
        }
        viewModel.reset()
    }

    @Test
    fun lateRegistrationCannotRestoreCallAfterReset() = runTest {
        val repository = FakeEmergencyCallRepository()
        val viewModel = createViewModel(repository)
        val deferredRegistration = CompletableDeferred<ApiResult<EmergencyCallStatus>>()
        repository.delayedRegistration = deferredRegistration
        viewModel.startEmergencyCall()
        runCurrent()
        viewModel.reset()
        deferredRegistration.complete(ApiResult.Success(callStatus("old_call", EmergencyCallState.ACTIVE)))
        runCurrent()
        assertNull(viewModel.uiState.value.callId)
        assertFalse(viewModel.uiState.value.isRegistering)
        assertEquals(0, repository.currentRequestCount)
    }

    @Test
    fun closedRegistrationResponseAllowsFreshIntentOnNextTap() = runTest {
        val repository = FakeEmergencyCallRepository()
        repository.registerResults += ApiResult.Success(callStatus("closed", EmergencyCallState.CANCELLED))
        val viewModel = createViewModel(repository)
        viewModel.startEmergencyCall()
        runCurrent()
        assertTrue(viewModel.uiState.value.hasRegistrationFailed)
        assertNull(viewModel.uiState.value.callId)
        viewModel.startEmergencyCall()
        runCurrent()
        assertTrue(repository.requestedIdempotencyKeys[0] != repository.requestedIdempotencyKeys[1])
        assertEquals("call_new", viewModel.uiState.value.callId)
        viewModel.reset()
    }

    private suspend fun TestScope.assertConflictRefreshFailure(error: ApiError) {
        val repository = FakeEmergencyCallRepository()
        val viewModel = createViewModel(repository)
        val receivedEffects = mutableListOf<EmergencyEffect>()
        backgroundScope.launch { viewModel.effects.collect { receivedEffects += it } }
        viewModel.startEmergencyCall()
        runCurrent()
        repository.currentCallResult = ApiResult.Failure(error)
        repository.updateResult = EmergencyCallUpdateResult.InvalidTransition
        viewModel.updateCallStatus(EmergencyCallUpdateStatus.CANCELLED)
        runCurrent()

        assertEquals("call_new", viewModel.uiState.value.callId)
        assertEquals(EmergencyCallState.ACTIVE, viewModel.uiState.value.callState)
        assertFalse(viewModel.uiState.value.isUpdatingStatus)
        assertTrue(viewModel.uiState.value.statusUpdateFailed)
        assertFalse(receivedEffects.any { it is EmergencyEffect.CallClosed })
        repository.currentCallResult = null
        repository.currentCall = callStatus("call_new", EmergencyCallState.ACKNOWLEDGED)
        advanceTimeBy(POLLING_INTERVAL_MS + 1)
        runCurrent()
        assertTrue(viewModel.uiState.value.isConnected)
        viewModel.reset()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `등록에 성공하면 호출 중 화면으로 보내고 폴링으로 관리자 확인을 반영한다`() = runTest {
        val repository = FakeEmergencyCallRepository()
        repository.currentCall = callStatus("call_new", EmergencyCallState.ACKNOWLEDGED)
        val viewModel = createViewModel(repository)

        viewModel.startEmergencyCall()
        runCurrent()

        assertTrue(viewModel.effects.first() is EmergencyEffect.CallStarted)
        assertEquals("call_new", viewModel.uiState.value.callId)
        assertTrue(viewModel.uiState.value.isConnected)
        viewModel.reset()
    }

    @Test
    fun `등록에 실패하면 오류를 보여주고 다시 누르면 같은 Idempotency-Key로 재시도한다`() = runTest {
        val repository = FakeEmergencyCallRepository()
        repository.registerResults += ApiResult.Failure(ApiError.Network)
        val viewModel = createViewModel(repository)

        viewModel.startEmergencyCall()
        runCurrent()
        assertTrue(viewModel.uiState.value.hasRegistrationFailed)
        assertNull(viewModel.uiState.value.callId)

        viewModel.startEmergencyCall()
        runCurrent()

        assertEquals(2, repository.requestedIdempotencyKeys.size)
        assertEquals(repository.requestedIdempotencyKeys[0], repository.requestedIdempotencyKeys[1])
        assertEquals("call_new", viewModel.uiState.value.callId)
        viewModel.reset()
    }

    @Test
    fun `이미 진행 중인 호출이 있으면(409) 현재 호출을 이어받는다`() = runTest {
        val repository = FakeEmergencyCallRepository()
        repository.registerResults += ApiResult.Failure(
            ApiError.Http(statusCode = 409, errorCode = "ACTIVE_CALL_ALREADY_EXISTS"),
        )
        repository.currentCall = callStatus("call_existing", EmergencyCallState.ACTIVE)
        val viewModel = createViewModel(repository)

        viewModel.startEmergencyCall()
        runCurrent()

        assertEquals("call_existing", viewModel.uiState.value.callId)
        assertTrue(viewModel.effects.first() is EmergencyEffect.CallStarted)
        viewModel.reset()
    }

    @Test
    fun `폴링 응답이 다른 호출이면 무시하고, 내 호출이 종료되면 흐름을 끝낸다`() = runTest {
        val repository = FakeEmergencyCallRepository()
        repository.currentCall = callStatus("call_old", EmergencyCallState.CANCELLED)
        val viewModel = createViewModel(repository)
        viewModel.startEmergencyCall()
        runCurrent()
        viewModel.effects.first()

        assertEquals("call_new", viewModel.uiState.value.callId)
        assertEquals(EmergencyCallState.ACTIVE, viewModel.uiState.value.callState)

        repository.currentCall = callStatus("call_new", EmergencyCallState.COMPLETED)
        advanceTimeBy(POLLING_INTERVAL_MS + 1)

        assertTrue(viewModel.effects.first() is EmergencyEffect.CallClosed)
        assertNull(viewModel.uiState.value.callId)
    }

    @Test
    fun `폴링이 세 번 연속 실패하면 연결 불안정을 알린다`() = runTest {
        val repository = FakeEmergencyCallRepository()
        repository.currentCallResult = ApiResult.Failure(ApiError.Network)
        val viewModel = createViewModel(repository)
        viewModel.startEmergencyCall()
        runCurrent()
        assertFalse(viewModel.uiState.value.isConnectionUnstable)

        advanceTimeBy(POLLING_INTERVAL_MS * 2 + 1)

        assertTrue(viewModel.uiState.value.isConnectionUnstable)
        viewModel.reset()
    }

    @Test
    fun `관리자가 확인한 호출은 취소를 눌러도 종료로 보낸다`() = runTest {
        val repository = FakeEmergencyCallRepository()
        repository.currentCall = callStatus("call_new", EmergencyCallState.ACKNOWLEDGED)
        val viewModel = createViewModel(repository)
        viewModel.startEmergencyCall()
        runCurrent()

        viewModel.updateCallStatus(EmergencyCallUpdateStatus.CANCELLED)
        runCurrent()

        assertEquals(listOf(EmergencyCallUpdateStatus.COMPLETED), repository.requestedUpdates)
        viewModel.reset()
    }

    @Test
    fun `취소 사이 관리자가 확인해 전이 불가(409)가 오면 최신 상태로 화면을 맞추고 흐름을 유지한다`() = runTest {
        val repository = FakeEmergencyCallRepository()
        repository.currentCall = callStatus("call_new", EmergencyCallState.ACTIVE)
        val viewModel = createViewModel(repository)
        viewModel.startEmergencyCall()
        runCurrent()

        repository.currentCall = callStatus("call_new", EmergencyCallState.ACKNOWLEDGED)
        repository.updateResult = EmergencyCallUpdateResult.InvalidTransition
        viewModel.updateCallStatus(EmergencyCallUpdateStatus.CANCELLED)
        runCurrent()

        val uiState = viewModel.uiState.value
        assertEquals("call_new", uiState.callId)
        assertEquals(EmergencyCallState.ACKNOWLEDGED, uiState.callState)
        assertEquals(false, uiState.isUpdatingStatus)
        assertEquals(false, uiState.statusUpdateFailed)
        viewModel.reset()
    }

    private fun callStatus(
        callId: String,
        callState: EmergencyCallState,
    ) = EmergencyCallStatus(
        callId = callId,
        state = callState,
        acknowledgedAt = null,
    )

    private fun TestScope.createViewModel(
        repository: FakeEmergencyCallRepository,
        sessionManager: SessionManager = createUnauthenticatedTestSessionManager(),
    ): EmergencyViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return EmergencyViewModel(
            registerEmergencyCallUseCase = RegisterEmergencyCallUseCase(repository),
            observeEmergencyCallStatusUseCase = ObserveEmergencyCallStatusUseCase(repository),
            updateEmergencyCallStatusUseCase = UpdateEmergencyCallStatusUseCase(repository),
            getCurrentEmergencyCallUseCase = GetCurrentEmergencyCallUseCase(repository),
            sessionManager = sessionManager,
            clock = Clock.fixed(Instant.parse("2026-09-28T03:00:00Z"), ZoneId.of("Asia/Seoul")),
        )
    }

    private class FakeEmergencyCallRepository : EmergencyCallRepository {
        var delayedUpdate: CompletableDeferred<EmergencyCallUpdateResult>? = null
        var delayedRegistration: CompletableDeferred<ApiResult<EmergencyCallStatus>>? = null
        val registerResults = ArrayDeque<ApiResult<EmergencyCallStatus>>()
        val requestedIdempotencyKeys = mutableListOf<String>()
        val requestedUpdates = mutableListOf<EmergencyCallUpdateStatus>()
        var currentCall: EmergencyCallStatus = EmergencyCallStatus(
            callId = "call_new",
            state = EmergencyCallState.ACTIVE,
            acknowledgedAt = null,
        )
        var currentCallResult: ApiResult<EmergencyCallStatus>? = null
        var currentRequestCount = 0

        override suspend fun registerEmergencyCall(
            idempotencyKey: String,
            clientOccurredAt: Instant,
            message: String?,
        ): ApiResult<EmergencyCallStatus> {
            requestedIdempotencyKeys += idempotencyKey
            val registration = delayedRegistration
            delayedRegistration = null
            if (registration != null) {
                return withContext(NonCancellable) { registration.await() }
            }
            return registerResults.removeFirstOrNull() ?: ApiResult.Success(
                EmergencyCallStatus(
                    callId = "call_new",
                    state = EmergencyCallState.ACTIVE,
                    acknowledgedAt = null,
                ),
            )
        }

        override suspend fun getCurrentEmergencyCallStatus(): ApiResult<EmergencyCallStatus> {
            currentRequestCount += 1
            return currentCallResult ?: ApiResult.Success(currentCall)
        }

        var updateResult: EmergencyCallUpdateResult? = null

        override suspend fun updateEmergencyCallStatus(
            callId: String,
            status: EmergencyCallUpdateStatus,
        ): EmergencyCallUpdateResult {
            requestedUpdates += status
            val update = delayedUpdate
            delayedUpdate = null
            if (update != null) {
                return withContext(NonCancellable) { update.await() }
            }
            return updateResult ?: EmergencyCallUpdateResult.Updated(
                EmergencyCallStatus(
                    callId = callId,
                    state = EmergencyCallState.COMPLETED,
                    acknowledgedAt = null,
                ),
            )
        }
    }

    private companion object {
        const val POLLING_INTERVAL_MS = 3_000L
    }
}
