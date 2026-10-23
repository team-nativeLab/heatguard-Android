package com.nativelap.heartguard.viewmodel.site

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.site.usecase.ClaimAppSiteLinkUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ClaimAppSiteLinkViewModel
    @Inject
    constructor(
        private val claimAppSiteLinkUseCase: ClaimAppSiteLinkUseCase,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow(ClaimAppSiteLinkUiState())

        val uiState: StateFlow<ClaimAppSiteLinkUiState> = mutableUiState.asStateFlow()

        fun claim(
            token: String,
            managerPhone: String,
        ) {
            val normalizedPhone = managerPhone.filter(Char::isDigit)
            if (mutableUiState.value.isSubmitting || token.isBlank() || normalizedPhone.isBlank()) {
                return
            }
            mutableUiState.update { it.copy(isSubmitting = true, failure = null) }
            viewModelScope.launch {
                when (val result = claimAppSiteLinkUseCase(token, normalizedPhone)) {
                    is ApiResult.Success -> {
                        mutableUiState.update {
                            it.copy(
                                isSubmitting = false,
                                isComplete = true,
                            )
                        }
                    }

                    is ApiResult.Failure -> {
                        mutableUiState.update {
                            it.copy(
                                isSubmitting = false,
                                failure = result.error.toClaimFailure(),
                            )
                        }
                    }
                }
            }
        }

        private fun ApiError.toClaimFailure(): ClaimAppSiteLinkFailure =
            when (this) {
                is ApiError.Http -> {
                    when {
                        errorCode == PHONE_MISMATCH_CODE -> ClaimAppSiteLinkFailure.PHONE_MISMATCH
                        statusCode == HTTP_NOT_FOUND -> ClaimAppSiteLinkFailure.LINK_UNAVAILABLE
                        statusCode == HTTP_TOO_MANY_REQUESTS -> ClaimAppSiteLinkFailure.RATE_LIMITED
                        else -> ClaimAppSiteLinkFailure.GENERIC
                    }
                }

                ApiError.Network -> {
                    ClaimAppSiteLinkFailure.NETWORK
                }

                ApiError.SessionChanged -> {
                    ClaimAppSiteLinkFailure.SESSION_REQUIRED
                }

                ApiError.Serialization,
                ApiError.Unknown,
                ApiError.LocalStorage,
                is ApiError.ServerRejected,
                -> {
                    ClaimAppSiteLinkFailure.GENERIC
                }
            }

        private companion object {
            const val PHONE_MISMATCH_CODE = "PHONE_MISMATCH"
            const val HTTP_NOT_FOUND = 404
            const val HTTP_TOO_MANY_REQUESTS = 429
        }
    }

internal data class ClaimAppSiteLinkUiState(
    val isSubmitting: Boolean = false,
    val isComplete: Boolean = false,
    val failure: ClaimAppSiteLinkFailure? = null,
)

internal enum class ClaimAppSiteLinkFailure {
    PHONE_MISMATCH,
    LINK_UNAVAILABLE,
    RATE_LIMITED,
    NETWORK,
    SESSION_REQUIRED,
    GENERIC,
}
