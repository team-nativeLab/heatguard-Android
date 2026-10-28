package com.nativelap.heartguard.viewmodel.emergency

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallState
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallStatus

internal sealed interface EmergencyCallResumeDecision {
    data object LookupFailed : EmergencyCallResumeDecision

    data class ActiveCall(
        val callStatus: EmergencyCallStatus,
    ) : EmergencyCallResumeDecision

    data object NoActiveCall : EmergencyCallResumeDecision
}

internal fun resolveEmergencyCallResumeDecision(result: ApiResult<EmergencyCallStatus>): EmergencyCallResumeDecision =
    when (result) {
        is ApiResult.Failure -> {
            EmergencyCallResumeDecision.LookupFailed
        }

        is ApiResult.Success -> {
            val callStatus = result.value
            val isInProgress =
                callStatus.state == EmergencyCallState.ACTIVE ||
                    callStatus.state == EmergencyCallState.ACKNOWLEDGED

            if (!callStatus.callId.isNullOrBlank() && isInProgress) {
                EmergencyCallResumeDecision.ActiveCall(callStatus)
            } else {
                EmergencyCallResumeDecision.NoActiveCall
            }
        }
    }
