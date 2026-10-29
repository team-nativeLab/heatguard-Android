package com.nativelap.heartguard.viewmodel.emergency

import com.nativelap.heartguard.core.network.ApiError
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmergencyRegistrationErrorTest {
    @Test
    fun explicitRejectionDoesNotAssumeCallWasCreated() {
        assertFalse(ApiError.Http(400).mayHaveCreatedCall())
        assertFalse(ApiError.ServerRejected("REJECTED").mayHaveCreatedCall())
        assertFalse(ApiError.SiteNotSelected.mayHaveCreatedCall())
    }

    @Test
    fun earlierUnknownResultSurvivesLaterExplicitRejection() {
        val uncertainAfterTimeout = registrationMayExistAfterFailure(false, ApiError.Network)
        assertTrue(uncertainAfterTimeout)
        assertTrue(registrationMayExistAfterFailure(uncertainAfterTimeout, ApiError.Http(400)))
        assertFalse(registrationMayExistAfterFailure(false, ApiError.Http(400)))
    }

    @Test
    fun pendingDismissClosesOnlyWhenNoEarlierOrCurrentRequestMayHaveCreatedCall() {
        assertTrue(shouldDismissAfterRegistrationFailure(true, false, ApiError.Http(400)))
        assertFalse(shouldDismissAfterRegistrationFailure(false, false, ApiError.Http(400)))
        assertFalse(shouldDismissAfterRegistrationFailure(true, true, ApiError.Http(400)))
        assertFalse(shouldDismissAfterRegistrationFailure(true, false, ApiError.Network))
    }

    @Test
    fun timeoutConflictAndInvalidResponseRequireRecovery() {
        assertTrue(ApiError.Network.mayHaveCreatedCall())
        assertTrue(ApiError.Http(500).mayHaveCreatedCall())
        assertTrue(ApiError.Http(408).mayHaveCreatedCall())
        assertTrue(ApiError.Http(409).mayHaveCreatedCall())
        assertTrue(ApiError.Serialization.mayHaveCreatedCall())
    }
}
