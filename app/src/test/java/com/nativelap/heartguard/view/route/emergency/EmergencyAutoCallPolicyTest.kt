package com.nativelap.heartguard.view.route.emergency

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallState
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallStatus
import com.nativelap.heartguard.viewmodel.emergency.EmergencyCallResumeDecision
import com.nativelap.heartguard.viewmodel.emergency.resolveAutoCallEligibleCallId
import com.nativelap.heartguard.viewmodel.emergency.resolveEmergencyCallResumeDecision
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EmergencyAutoCallPolicyTest {
    @Test
    fun doesNotStartWithoutAnActiveCallId() {
        assertFalse(
            shouldStartEmergencyAutoCall(
                callId = null,
                autoCallEligibleCallId = null,
                isConnected = true,
                canControlCall = true,
                phoneNumber = "010-1234-5678",
                lastAttemptedCallId = null,
            ),
        )
    }

    @Test
    fun doesNotStartBeforeCallIsConnectedAndControllable() {
        assertFalse(
            shouldStartEmergencyAutoCall(
                callId = "call-1",
                autoCallEligibleCallId = "call-1",
                isConnected = false,
                canControlCall = true,
                phoneNumber = "010-1234-5678",
                lastAttemptedCallId = null,
            ),
        )
        assertFalse(
            shouldStartEmergencyAutoCall(
                callId = "call-1",
                autoCallEligibleCallId = "call-1",
                isConnected = true,
                canControlCall = false,
                phoneNumber = "010-1234-5678",
                lastAttemptedCallId = null,
            ),
        )
    }

    @Test
    fun waitsForAUsablePhoneNumber() {
        assertFalse(
            shouldStartEmergencyAutoCall(
                callId = "call-1",
                autoCallEligibleCallId = "call-1",
                isConnected = true,
                canControlCall = true,
                phoneNumber = null,
                lastAttemptedCallId = null,
            ),
        )
        assertFalse(
            shouldStartEmergencyAutoCall(
                callId = "call-1",
                autoCallEligibleCallId = "call-1",
                isConnected = true,
                canControlCall = true,
                phoneNumber = "---",
                lastAttemptedCallId = null,
            ),
        )
    }

    @Test
    fun startsOncePerActiveCallId() {
        val shouldStartFirstAttempt =
            shouldStartEmergencyAutoCall(
                callId = "call-1",
                autoCallEligibleCallId = "call-1",
                isConnected = true,
                canControlCall = true,
                phoneNumber = "+82 10-1234-5678",
                lastAttemptedCallId = null,
            )
        val shouldSkipRepeatedAttempt =
            shouldStartEmergencyAutoCall(
                callId = "call-1",
                autoCallEligibleCallId = "call-1",
                isConnected = true,
                canControlCall = true,
                phoneNumber = "+82 10-1234-5678",
                lastAttemptedCallId = "call-1",
            )
        val shouldStartDifferentCall =
            shouldStartEmergencyAutoCall(
                callId = "call-2",
                autoCallEligibleCallId = "call-2",
                isConnected = true,
                canControlCall = true,
                phoneNumber = "+82 10-1234-5678",
                lastAttemptedCallId = "call-1",
            )

        assertTrue(shouldStartFirstAttempt)
        assertFalse(shouldSkipRepeatedAttempt)
        assertTrue(shouldStartDifferentCall)
    }

    @Test
    fun doesNotAutoCallACallOriginatedOnAnotherDevice() {
        assertFalse(
            shouldStartEmergencyAutoCall(
                callId = "call-2",
                autoCallEligibleCallId = null,
                isConnected = true,
                canControlCall = true,
                phoneNumber = "010-1234-5678",
                lastAttemptedCallId = null,
            ),
        )
        assertFalse(
            canAttemptEmergencyAutoCall(
                callId = "call-2",
                autoCallEligibleCallId = null,
                isConnected = true,
                canControlCall = true,
            ),
        )
    }

    @Test
    fun resolvesAutoCallOriginForRegistrationAndRestoration() {
        val newlyRegisteredCallId =
            resolveAutoCallEligibleCallId(
                callId = "call-1",
                wasRegisteredOnThisDevice = true,
                savedOriginCallId = null,
            )
        val restoredCallId =
            resolveAutoCallEligibleCallId(
                callId = "call-1",
                wasRegisteredOnThisDevice = false,
                savedOriginCallId = "call-1",
            )
        val otherDeviceCallId =
            resolveAutoCallEligibleCallId(
                callId = "call-2",
                wasRegisteredOnThisDevice = false,
                savedOriginCallId = "call-1",
            )

        assertEquals("call-1", newlyRegisteredCallId)
        assertEquals("call-1", restoredCallId)
        assertNull(otherDeviceCallId)
    }

    @Test
    fun allowsAutoCallAfterTransientResumeFailureAndSuccessfulRetry() {
        val savedOriginCallId = "call-1"
        val failedLookupDecision =
            resolveEmergencyCallResumeDecision(
                result = ApiResult.Failure(ApiError.Network),
                savedOriginCallId = savedOriginCallId,
            )

        assertEquals(
            EmergencyCallResumeDecision.LookupFailed(savedOriginCallId),
            failedLookupDecision,
        )
        val preservedOriginCallId =
            (failedLookupDecision as EmergencyCallResumeDecision.LookupFailed).preservedOriginCallId

        val retryDecision =
            resolveEmergencyCallResumeDecision(
                result =
                    ApiResult.Success(
                        EmergencyCallStatus(
                            callId = savedOriginCallId,
                            state = EmergencyCallState.ACKNOWLEDGED,
                            acknowledgedAt = null,
                        ),
                    ),
                savedOriginCallId = preservedOriginCallId,
            )
        val retryCallId =
            when (retryDecision) {
                is EmergencyCallResumeDecision.ActiveCall -> {
                    resolveAutoCallEligibleCallId(
                        callId = retryDecision.callStatus.callId,
                        wasRegisteredOnThisDevice = false,
                        savedOriginCallId = preservedOriginCallId,
                    )
                }

                else -> {
                    null
                }
            }

        assertEquals("call-1", retryCallId)
        assertTrue(
            shouldStartEmergencyAutoCall(
                callId = retryCallId,
                autoCallEligibleCallId = retryCallId,
                isConnected = true,
                canControlCall = true,
                phoneNumber = "+82 10-1234-5678",
                lastAttemptedCallId = null,
            ),
        )
    }

    @Test
    fun clearsAutoCallOriginWhenSuccessfulResumeFindsNoActiveCall() {
        val noActiveCallDecision =
            resolveEmergencyCallResumeDecision(
                result =
                    ApiResult.Success(
                        EmergencyCallStatus(
                            callId = null,
                            state = EmergencyCallState.NONE,
                            acknowledgedAt = null,
                        ),
                    ),
                savedOriginCallId = "call-1",
            )

        assertEquals(EmergencyCallResumeDecision.NoActiveCall, noActiveCallDecision)
    }
}
