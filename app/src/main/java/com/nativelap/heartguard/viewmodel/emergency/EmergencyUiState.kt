package com.nativelap.heartguard.viewmodel.emergency

import com.nativelap.heartguard.domain.emergency.model.EmergencyCallState
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallStatus

data class EmergencyUiState(
    val status: EmergencyCallStatus = EmergencyCallStatus(
        callId = null,
        state = EmergencyCallState.NONE,
        acknowledgedAt = null,
    ),
)
