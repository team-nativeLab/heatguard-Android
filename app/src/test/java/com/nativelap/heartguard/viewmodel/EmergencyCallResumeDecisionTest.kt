package com.nativelap.heartguard.viewmodel.emergency

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallState
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class EmergencyCallResumeDecisionTest {
    @Test
    fun keepsLookupFailureSeparateFromNoActiveCall() {
        assertEquals(
            EmergencyCallResumeDecision.LookupFailed,
            resolveEmergencyCallResumeDecision(ApiResult.Failure(ApiError.Network)),
        )
    }

    @Test
    fun resumesActiveOrAcknowledgedCall() {
        val activeStatus = emergencyCallStatus("call-1", EmergencyCallState.ACTIVE)
        val acknowledgedStatus = emergencyCallStatus("call-2", EmergencyCallState.ACKNOWLEDGED)

        assertEquals(
            EmergencyCallResumeDecision.ActiveCall(activeStatus),
            resolveEmergencyCallResumeDecision(ApiResult.Success(activeStatus)),
        )
        assertEquals(
            EmergencyCallResumeDecision.ActiveCall(acknowledgedStatus),
            resolveEmergencyCallResumeDecision(ApiResult.Success(acknowledgedStatus)),
        )
    }

    @Test
    fun doesNotResumeMissingOrClosedCalls() {
        val noCall = emergencyCallStatus(null, EmergencyCallState.NONE)
        val completedCall = emergencyCallStatus("call-3", EmergencyCallState.COMPLETED)

        assertEquals(
            EmergencyCallResumeDecision.NoActiveCall,
            resolveEmergencyCallResumeDecision(ApiResult.Success(noCall)),
        )
        assertEquals(
            EmergencyCallResumeDecision.NoActiveCall,
            resolveEmergencyCallResumeDecision(ApiResult.Success(completedCall)),
        )
    }
}

private fun emergencyCallStatus(
    callId: String?,
    state: EmergencyCallState,
) = EmergencyCallStatus(
    callId = callId,
    state = state,
    acknowledgedAt = null,
)
