package com.nativelap.heartguard.domain.emergency

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallState
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallStatus
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallUpdateResult
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallUpdateStatus
import com.nativelap.heartguard.domain.emergency.model.OwnedEmergencyCall
import com.nativelap.heartguard.domain.emergency.repository.EmergencyCallRepository
import com.nativelap.heartguard.domain.emergency.usecase.EndActiveEmergencyCallResult
import com.nativelap.heartguard.domain.emergency.usecase.EndActiveEmergencyCallUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EndActiveEmergencyCallUseCaseTest {
    @Test
    fun `an owned active call is cancelled and released`() =
        runTest {
            val owned = OwnedEmergencyCall().apply { own("my-call") }
            val repository = FakeRepository(updateResults = mutableListOf(EmergencyCallUpdateResult.AlreadyClosed))

            val result = EndActiveEmergencyCallUseCase(repository, owned).invoke()

            assertEquals(listOf("my-call" to EmergencyCallUpdateStatus.CANCELLED), repository.updateRequests)
            assertEquals(EndActiveEmergencyCallResult.Ended, result)
            assertNull(owned.current())
        }

    @Test
    fun `an acknowledged call is ended with completion after the cancel is rejected`() =
        runTest {
            val owned = OwnedEmergencyCall().apply { own("my-call") }
            val repository =
                FakeRepository(
                    updateResults =
                        mutableListOf(
                            EmergencyCallUpdateResult.InvalidTransition,
                            EmergencyCallUpdateResult.AlreadyClosed,
                        ),
                )

            val result = EndActiveEmergencyCallUseCase(repository, owned).invoke()

            assertEquals(
                listOf(
                    "my-call" to EmergencyCallUpdateStatus.CANCELLED,
                    "my-call" to EmergencyCallUpdateStatus.COMPLETED,
                ),
                repository.updateRequests,
            )
            assertEquals(EndActiveEmergencyCallResult.Ended, result)
        }

    @Test
    fun `a colleague's call is never touched when this device owns no call`() =
        runTest {
            val repository = FakeRepository()

            val result = EndActiveEmergencyCallUseCase(repository, OwnedEmergencyCall()).invoke()

            assertTrue(repository.updateRequests.isEmpty())
            assertEquals(EndActiveEmergencyCallResult.NoActiveCall, result)
        }

    @Test
    fun `a failed request leaves the owned call unconfirmed so logout can be blocked`() =
        runTest {
            val owned = OwnedEmergencyCall().apply { own("my-call") }
            val repository = FakeRepository(updateResults = mutableListOf(EmergencyCallUpdateResult.Failure))

            val result = EndActiveEmergencyCallUseCase(repository, owned).invoke()

            assertEquals(EndActiveEmergencyCallResult.Unconfirmed, result)
            assertEquals("my-call", owned.current())
        }

    @Test
    fun `an update response that is not closed keeps the owned call and blocks logout`() =
        runTest {
            val owned = OwnedEmergencyCall().apply { own("my-call") }
            val repository =
                FakeRepository(
                    updateResults =
                        mutableListOf(
                            EmergencyCallUpdateResult.Updated(
                                EmergencyCallStatus(
                                    callId = "my-call",
                                    state = EmergencyCallState.ACTIVE,
                                    acknowledgedAt = null,
                                ),
                            ),
                        ),
                )

            val result = EndActiveEmergencyCallUseCase(repository, owned).invoke()

            assertEquals(EndActiveEmergencyCallResult.Unconfirmed, result)
            assertEquals("my-call", owned.current())
        }

    private class FakeRepository(
        private val updateResults: MutableList<EmergencyCallUpdateResult> = mutableListOf(),
    ) : EmergencyCallRepository {
        val updateRequests = mutableListOf<Pair<String, EmergencyCallUpdateStatus>>()

        override suspend fun registerEmergencyCall(
            idempotencyKey: String,
            clientOccurredAt: Instant,
            message: String?,
        ): ApiResult<EmergencyCallStatus> = error("register is unused in this test")

        override suspend fun getCurrentEmergencyCallStatus(): ApiResult<EmergencyCallStatus> =
            ApiResult.Failure(ApiError.Network)

        override suspend fun updateEmergencyCallStatus(
            callId: String,
            status: EmergencyCallUpdateStatus,
        ): EmergencyCallUpdateResult {
            updateRequests.add(callId to status)
            return if (updateResults.isEmpty()) {
                EmergencyCallUpdateResult.AlreadyClosed
            } else {
                updateResults.removeAt(0)
            }
        }
    }
}
