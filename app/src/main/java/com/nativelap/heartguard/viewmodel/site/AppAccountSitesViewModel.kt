package com.nativelap.heartguard.viewmodel.site

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.domain.auth.usecase.LogoutAppAccountUseCase
import com.nativelap.heartguard.domain.site.model.AppLinkedSite
import com.nativelap.heartguard.domain.site.usecase.GetAppLinkedSitesUseCase
import com.nativelap.heartguard.domain.site.usecase.GetSelectedAppSiteIdUseCase
import com.nativelap.heartguard.domain.site.usecase.SelectAppSiteUseCase
import com.nativelap.heartguard.domain.site.usecase.UnlinkAppSiteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class AppAccountSitesViewModel
    @Inject
    constructor(
        private val getAppLinkedSitesUseCase: GetAppLinkedSitesUseCase,
        private val getSelectedAppSiteIdUseCase: GetSelectedAppSiteIdUseCase,
        private val selectAppSiteUseCase: SelectAppSiteUseCase,
        private val unlinkAppSiteUseCase: UnlinkAppSiteUseCase,
        private val logoutAppAccountUseCase: LogoutAppAccountUseCase,
        private val sessionManager: SessionManager,
    ) : ViewModel() {
        private val mutableUiState =
            MutableStateFlow(
                AppAccountSitesUiState(
                    selectedSiteId = getSelectedAppSiteIdUseCase(),
                ),
            )

        val uiState: StateFlow<AppAccountSitesUiState> = mutableUiState.asStateFlow()

        init {
            refresh()
        }

        fun refresh() {
            if (isBusy()) {
                return
            }
            mutableUiState.update { it.copy(isLoading = true, hasFailure = false) }
            viewModelScope.launch {
                when (val result = getAppLinkedSitesUseCase()) {
                    is ApiResult.Success -> {
                        mutableUiState.update {
                            it.copy(
                                isLoading = false,
                                linkedSites = result.value,
                                selectedSiteId = getSelectedAppSiteIdUseCase(),
                            )
                        }
                    }

                    is ApiResult.Failure -> {
                        mutableUiState.update { it.copy(isLoading = false, hasFailure = true) }
                    }
                }
            }
        }

        fun selectSite(siteId: String) {
            if (isBusy()) {
                return
            }
            mutableUiState.update { it.copy(isSelectingSite = true, hasFailure = false) }
            viewModelScope.launch {
                when (val result = selectAppSiteUseCase(siteId)) {
                    is ApiResult.Success -> {
                        mutableUiState.update {
                            it.copy(
                                isSelectingSite = false,
                                selectedSiteId = result.value.siteId,
                            )
                        }
                    }

                    is ApiResult.Failure -> {
                        mutableUiState.update { it.copy(isSelectingSite = false, hasFailure = true) }
                    }
                }
            }
        }

        private fun isBusy(): Boolean =
            mutableUiState.value.let { it.isLoading || it.isSelectingSite || it.isUnlinkingSite }

        fun unlinkSite(siteId: String) {
            if (isBusy()) {
                return
            }
            mutableUiState.update { it.copy(isUnlinkingSite = true, hasUnlinkFailure = false) }
            viewModelScope.launch {
                when (unlinkAppSiteUseCase(siteId)) {
                    is ApiResult.Success -> {
                        mutableUiState.update {
                            it.copy(
                                isUnlinkingSite = false,
                                linkedSites = it.linkedSites.filterNot { site -> site.siteId == siteId },
                                selectedSiteId = getSelectedAppSiteIdUseCase(),
                            )
                        }
                        refresh()
                    }

                    is ApiResult.Failure -> {
                        mutableUiState.update { it.copy(isUnlinkingSite = false, hasUnlinkFailure = true) }
                        refresh()
                    }
                }
            }
        }

        fun logout() {
            viewModelScope.launch {
                logoutAppAccountUseCase()
                sessionManager.expireSession()
            }
        }
    }

internal data class AppAccountSitesUiState(
    val linkedSites: List<AppLinkedSite> = emptyList(),
    val selectedSiteId: String? = null,
    val isLoading: Boolean = false,
    val isSelectingSite: Boolean = false,
    val isUnlinkingSite: Boolean = false,
    val hasUnlinkFailure: Boolean = false,
    val hasFailure: Boolean = false,
)
