package com.nativelap.heartguard.viewmodel.emergency

import com.nativelap.heartguard.core.network.ApiError

internal fun ApiError.mayHaveCreatedCall(): Boolean =
    when (this) {
        is ApiError.Http -> statusCode !in 400..499 || statusCode == 408 || statusCode == 409

        is ApiError.ServerRejected,
        ApiError.SessionChanged,
        ApiError.SiteNotSelected,
        -> false

        ApiError.Network,
        ApiError.Serialization,
        ApiError.LocalStorage,
        ApiError.Unknown,
        -> true
    }

internal fun registrationMayExistAfterFailure(
    previousMayExist: Boolean,
    error: ApiError,
): Boolean = previousMayExist || error.mayHaveCreatedCall()

internal fun shouldDismissAfterRegistrationFailure(
    dismissRequested: Boolean,
    previousMayExist: Boolean,
    error: ApiError,
): Boolean = dismissRequested && !registrationMayExistAfterFailure(previousMayExist, error)
