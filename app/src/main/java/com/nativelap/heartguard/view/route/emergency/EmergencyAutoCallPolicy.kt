package com.nativelap.heartguard.view.route.emergency

import com.nativelap.heartguard.core.util.isValidPhoneNumber
import com.nativelap.heartguard.viewmodel.emergency.resolveAutoCallEligibleCallId

internal fun shouldStartEmergencyAutoCall(
    callId: String?,
    autoCallEligibleCallId: String?,
    isConnected: Boolean,
    canControlCall: Boolean,
    phoneNumber: String?,
    lastAttemptedCallId: String?,
): Boolean =
    canAttemptEmergencyAutoCall(
        callId = callId,
        autoCallEligibleCallId = autoCallEligibleCallId,
        isConnected = isConnected,
        canControlCall = canControlCall,
    ) &&
        phoneNumber?.isValidPhoneNumber() == true &&
        lastAttemptedCallId != callId

internal fun canAttemptEmergencyAutoCall(
    callId: String?,
    autoCallEligibleCallId: String?,
    isConnected: Boolean,
    canControlCall: Boolean,
): Boolean =
    !callId.isNullOrBlank() &&
        autoCallEligibleCallId == callId &&
        isConnected &&
        canControlCall
