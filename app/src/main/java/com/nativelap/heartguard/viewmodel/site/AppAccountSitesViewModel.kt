package com.nativelap.heartguard.viewmodel.site

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionRole
import com.nativelap.heartguard.core.session.SessionSnapshot
import com.nativelap.heartguard.core.session.SessionState
import com.nativelap.heartguard.domain.auth.usecase.LogoutAppAccountUseCase
import com.nativelap.heartguard.domain.site.model.AppLinkedSite
import com.nativelap.heartguard.domain.site.model.AppSiteDashboard
import com.nativelap.heartguard.domain.site.usecase.GetAppLinkedSitesUseCase
import com.nativelap.heartguard.domain.site.usecase.GetAppSiteDashboardUseCase
import com.nativelap.heartguard.domain.site.usecase.GetSelectedAppSiteIdUseCase
import com.nativelap.heartguard.domain.site.usecase.SelectAppSiteUseCase
import com.nativelap.heartguard.domain.site.usecase.UnlinkAppSiteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
        private val getAppSiteDashboardUseCase: GetAppSiteDashboardUseCase,
        private val logoutAppAccountUseCase: LogoutAppAccountUseCase,
        private val sessionManager: SessionManager,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow(AppAccountSitesUiState())

        val uiState: StateFlow<AppAccountSitesUiState> = mutableUiState.asStateFlow()

        private var activeSessionSnapshot: SessionSnapshot? = null
        private val sessionRequestJobs = mutableSetOf<Job>()

        init {
            viewModelScope.launch {
                combine(
                    sessionManager.sessionState,
                    sessionManager.sessionSnapshot,
                ) { sessionState, sessionSnapshot ->
                    SessionOwner(sessionState, sessionSnapshot)
                }.distinctUntilChanged()
                    .collect(::onSessionChanged)
            }
        }

        fun refresh() {
            activeSessionSnapshot?.let(::refresh)
        }

        private fun refresh(expectedSession: SessionSnapshot) {
            if (!isCurrentSession(expectedSession) || isBusy()) {
                return
            }
            mutableUiState.update { it.copy(isLoading = true, hasFailure = false, dashboard = null) }
            launchSessionRequest(expectedSession) {
                when (val result = getAppLinkedSitesUseCase()) {
                    is ApiResult.Success -> {
                        if (!isCurrentSession(expectedSession)) {
                            return@launchSessionRequest
                        }
                        mutableUiState.update {
                            it.copy(
                                isLoading = false,
                                linkedSites = result.value,
                                selectedSiteId = getSelectedAppSiteIdUseCase(),
                                openedSiteId =
                                    it.openedSiteId?.takeIf { openedId ->
                                        result.value.any { site -> site.siteId == openedId }
                                    },
                            )
                        }
                        mutableUiState.value.openedSiteId?.let { siteId ->
                            loadDashboard(siteId, expectedSession)
                        }
                    }

                    is ApiResult.Failure -> {
                        if (isCurrentSession(expectedSession)) {
                            mutableUiState.update {
                                it.copy(isLoading = false, hasFailure = true, dashboard = null)
                            }
                        }
                    }
                }
            }
        }

        fun selectSite(siteId: String) {
            val expectedSession = activeSessionSnapshot ?: return
            if (!isCurrentSession(expectedSession) || isBusy()) {
                return
            }
            mutableUiState.update { it.copy(isSelectingSite = true, hasFailure = false) }
            launchSessionRequest(expectedSession) {
                when (val result = selectAppSiteUseCase(siteId)) {
                    is ApiResult.Success -> {
                        if (!isCurrentSession(expectedSession)) {
                            return@launchSessionRequest
                        }
                        mutableUiState.update {
                            it.copy(
                                isSelectingSite = false,
                                selectedSiteId = result.value.siteId,
                                openedSiteId = result.value.siteId,
                                dashboard = null,
                            )
                        }
                        loadDashboard(result.value.siteId, expectedSession)
                    }

                    is ApiResult.Failure -> {
                        if (isCurrentSession(expectedSession)) {
                            mutableUiState.update {
                                it.copy(isSelectingSite = false, hasFailure = true)
                            }
                        }
                    }
                }
            }
        }

        private fun isBusy(): Boolean =
            mutableUiState.value.let {
                it.isLoading || it.isSelectingSite || it.isUnlinkingSite || it.isLoadingDashboard
            }

        private suspend fun loadDashboard(
            siteId: String,
            expectedSession: SessionSnapshot,
        ) {
            if (!isCurrentSession(expectedSession)) {
                return
            }
            mutableUiState.update {
                it.copy(isLoadingDashboard = true, dashboard = null, hasDashboardFailure = false)
            }
            when (val result = getAppSiteDashboardUseCase(siteId)) {
                is ApiResult.Success -> {
                    if (isCurrentSession(expectedSession)) {
                        mutableUiState.update {
                            it.copy(isLoadingDashboard = false, dashboard = result.value)
                        }
                    }
                }

                is ApiResult.Failure -> {
                    if (isCurrentSession(expectedSession)) {
                        mutableUiState.update {
                            it.copy(
                                isLoadingDashboard = false,
                                dashboard = null,
                                hasDashboardFailure = true,
                            )
                        }
                    }
                }
            }
        }

        fun closeDashboard() {
            if (!isBusy()) {
                mutableUiState.update {
                    it.copy(openedSiteId = null, dashboard = null, hasDashboardFailure = false)
                }
            }
        }

        fun unlinkSite(siteId: String) {
            val expectedSession = activeSessionSnapshot ?: return
            if (!isCurrentSession(expectedSession) || isBusy()) {
                return
            }
            mutableUiState.update { it.copy(isUnlinkingSite = true, hasUnlinkFailure = false) }
            launchSessionRequest(expectedSession) {
                when (unlinkAppSiteUseCase(siteId)) {
                    is ApiResult.Success -> {
                        if (!isCurrentSession(expectedSession)) {
                            return@launchSessionRequest
                        }
                        mutableUiState.update {
                            it.copy(
                                isUnlinkingSite = false,
                                linkedSites = it.linkedSites.filterNot { site -> site.siteId == siteId },
                                selectedSiteId = getSelectedAppSiteIdUseCase(),
                                openedSiteId = it.openedSiteId?.takeUnless { openedId -> openedId == siteId },
                                dashboard = null,
                            )
                        }
                        refresh(expectedSession)
                    }

                    is ApiResult.Failure -> {
                        if (isCurrentSession(expectedSession)) {
                            mutableUiState.update {
                                it.copy(isUnlinkingSite = false, hasUnlinkFailure = true)
                            }
                            refresh(expectedSession)
                        }
                    }
                }
            }
        }

        fun logout() {
            val expectedSession = activeSessionSnapshot ?: return
            if (!isCurrentSession(expectedSession)) {
                return
            }
            launchSessionRequest(expectedSession) {
                logoutAppAccountUseCase()
                if (isCurrentSession(expectedSession)) {
                    sessionManager.expireSession(expectedSession.generation)
                }
            }
        }

        private fun onSessionChanged(owner: SessionOwner) {
            val isAppAccountSession =
                owner.state == SessionState.Authenticated &&
                    owner.snapshot.role == SessionRole.APP_ACCOUNT &&
                    !owner.snapshot.accessToken.isNullOrBlank()
            if (!isAppAccountSession) {
                clearSessionState()
                return
            }
            if (activeSessionSnapshot == owner.snapshot) {
                return
            }

            cancelSessionRequests()
            activeSessionSnapshot = owner.snapshot
            mutableUiState.value =
                AppAccountSitesUiState(
                    selectedSiteId = getSelectedAppSiteIdUseCase(),
                )
            refresh(owner.snapshot)
        }

        private fun clearSessionState() {
            if (activeSessionSnapshot == null && mutableUiState.value == AppAccountSitesUiState()) {
                return
            }
            cancelSessionRequests()
            activeSessionSnapshot = null
            mutableUiState.value = AppAccountSitesUiState()
        }

        private fun cancelSessionRequests() {
            sessionRequestJobs.toList().forEach(Job::cancel)
            sessionRequestJobs.clear()
        }

        private fun launchSessionRequest(
            expectedSession: SessionSnapshot,
            action: suspend () -> Unit,
        ) {
            val job =
                viewModelScope.launch {
                    if (isCurrentSession(expectedSession)) {
                        action()
                    }
                }
            sessionRequestJobs += job
            job.invokeOnCompletion {
                sessionRequestJobs -= job
            }
        }

        private fun isCurrentSession(expectedSession: SessionSnapshot): Boolean =
            activeSessionSnapshot == expectedSession &&
                sessionManager.getSnapshot() == expectedSession &&
                sessionManager.sessionState.value == SessionState.Authenticated &&
                expectedSession.role == SessionRole.APP_ACCOUNT
    }

private data class SessionOwner(
    val state: SessionState,
    val snapshot: SessionSnapshot,
)

internal data class AppAccountSitesUiState(
    val linkedSites: List<AppLinkedSite> = emptyList(),
    val selectedSiteId: String? = null,
    val isLoading: Boolean = false,
    val isSelectingSite: Boolean = false,
    val isUnlinkingSite: Boolean = false,
    val hasUnlinkFailure: Boolean = false,
    val openedSiteId: String? = null,
    val dashboard: AppSiteDashboard? = null,
    val isLoadingDashboard: Boolean = false,
    val hasDashboardFailure: Boolean = false,
    val hasFailure: Boolean = false,
)
