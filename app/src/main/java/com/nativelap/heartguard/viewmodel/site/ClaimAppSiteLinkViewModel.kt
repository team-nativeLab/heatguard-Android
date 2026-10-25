package com.nativelap.heartguard.viewmodel.site

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.PendingAppLink
import com.nativelap.heartguard.core.session.SessionManager
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
        private val sessionManager: SessionManager,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow(ClaimAppSiteLinkUiState())

        val uiState: StateFlow<ClaimAppSiteLinkUiState> = mutableUiState.asStateFlow()

        fun activateLink(
            pendingAppLink: PendingAppLink,
            clearSessionRequiredFailure: Boolean = false,
        ) {
            val currentState = mutableUiState.value
            if (currentState.linkId != pendingAppLink.linkId ||
                (clearSessionRequiredFailure && currentState.failure == ClaimAppSiteLinkFailure.SESSION_REQUIRED)
            ) {
                mutableUiState.value = ClaimAppSiteLinkUiState(linkId = pendingAppLink.linkId)
            }
        }

        fun claim(
            pendingAppLink: PendingAppLink,
            managerPhone: String,
        ) {
            val normalizedPhone = managerPhone.filter(Char::isDigit)
            val currentState = mutableUiState.value
            if (currentState.linkId != pendingAppLink.linkId ||
                currentState.isSubmitting ||
                currentState.completedLinkId != null ||
                pendingAppLink.token.isBlank() ||
                normalizedPhone.isBlank()
            ) {
                return
            }
            mutableUiState.update { it.copy(isSubmitting = true, failure = null) }
            val expectedSession = sessionManager.getSnapshot()
            viewModelScope.launch {
                when (
                    val result =
                        claimAppSiteLinkUseCase(
                            pendingAppLink.token,
                            normalizedPhone,
                            expectedSession.generation,
                        )
                ) {
                    is ApiResult.Success -> {
                        if (sessionManager.getSnapshot() != expectedSession) {
                            markSessionChanged(pendingAppLink.linkId)
                            return@launch
                        }
                        mutableUiState.update {
                            if (it.linkId == pendingAppLink.linkId) {
                                it.copy(
                                    isSubmitting = false,
                                    completedLinkId = pendingAppLink.linkId,
                                )
                            } else {
                                it
                            }
                        }
                    }

                    is ApiResult.Failure -> {
                        if (sessionManager.getSnapshot() != expectedSession) {
                            markSessionChanged(pendingAppLink.linkId)
                            return@launch
                        }
                        mutableUiState.update {
                            if (it.linkId == pendingAppLink.linkId) {
                                it.copy(
                                    isSubmitting = false,
                                    failure = result.error.toClaimFailure(),
                                )
                            } else {
                                it
                            }
                        }
                    }
                }
            }
        }

        private fun markSessionChanged(linkId: Long) {
            mutableUiState.update { state ->
                if (state.linkId == linkId && state.isSubmitting) {
                    state.copy(
                        isSubmitting = false,
                        failure = ClaimAppSiteLinkFailure.SESSION_REQUIRED,
                    )
                } else {
                    state
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

                ApiError.SiteNotSelected -> {
                    ClaimAppSiteLinkFailure.GENERIC
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
    val linkId: Long? = null,
    val isSubmitting: Boolean = false,
    val completedLinkId: Long? = null,
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
