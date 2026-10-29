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
import com.nativelap.heartguard.domain.site.usecase.SelectAppTeamUseCase
import com.nativelap.heartguard.domain.site.usecase.UnlinkAppTeamUseCase
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
        private val selectAppTeamUseCase: SelectAppTeamUseCase,
        private val unlinkAppTeamUseCase: UnlinkAppTeamUseCase,
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
                loadLinkedSites(expectedSession)
            }
        }

        private suspend fun loadLinkedSites(expectedSession: SessionSnapshot): Boolean {
            when (val result = getAppLinkedSitesUseCase(expectedSession.generation)) {
                is ApiResult.Success -> {
                    if (!isCurrentSession(expectedSession)) {
                        return false
                    }
                    mutableUiState.update {
                        it.copy(
                            isLoading = false,
                            hasUnlinkFailure = false,
                            linkedSites = result.value.items,
                            selectedTeamId = result.value.selectedTeamId,
                            selectedSiteId = getSelectedAppSiteIdUseCase(),
                            openedSiteId =
                                it.openedSiteId?.takeIf { openedId ->
                                    result.value.items.any { site -> site.siteId == openedId }
                                },
                        )
                    }
                    mutableUiState.value.openedSiteId?.let { siteId ->
                        loadDashboard(siteId, expectedSession)
                    }
                    return true
                }

                is ApiResult.Failure -> {
                    if (isCurrentSession(expectedSession)) {
                        mutableUiState.update {
                            it.copy(isLoading = false, hasFailure = true, dashboard = null)
                        }
                    }
                    return false
                }
            }
        }

        fun selectTeam(teamId: String) {
            val expectedSession = activeSessionSnapshot ?: return
            if (!isCurrentSession(expectedSession) || isBusy()) {
                return
            }
            mutableUiState.update {
                it.copy(
                    isSelectingSite = true,
                    selectingTeamId = teamId,
                    hasSelectionFailure = false,
                    hasUnlinkFailure = false,
                )
            }
            launchSessionRequest(expectedSession) {
                when (
                    val result =
                        selectAppTeamUseCase(
                            teamId,
                            expectedSession.generation,
                        )
                ) {
                    is ApiResult.Success -> {
                        if (!isCurrentSession(expectedSession)) {
                            return@launchSessionRequest
                        }
                        mutableUiState.update {
                            it.copy(
                                isSelectingSite = false,
                                selectingTeamId = null,
                                selectedTeamId = result.value.teamId,
                                selectedSiteId = result.value.siteId,
                                openedSiteId = null,
                                dashboard = null,
                            )
                        }
                    }

                    is ApiResult.Failure -> {
                        if (isCurrentSession(expectedSession)) {
                            mutableUiState.update {
                                it.copy(isSelectingSite = false, selectingTeamId = null, hasSelectionFailure = true)
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
            when (val result = getAppSiteDashboardUseCase(siteId, expectedSession.generation)) {
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

        fun openDashboard(siteId: String) {
            val expectedSession = activeSessionSnapshot ?: return
            if (!isCurrentSession(expectedSession) || isBusy() ||
                mutableUiState.value.selectedSiteId != siteId
            ) {
                return
            }
            mutableUiState.update { it.copy(openedSiteId = siteId) }
            viewModelScope.launch {
                loadDashboard(siteId, expectedSession)
            }
        }

        fun unlinkTeam(
            teamId: String,
            siteId: String,
        ) {
            val expectedSession = activeSessionSnapshot ?: return
            if (!isCurrentSession(expectedSession) || isBusy()) {
                return
            }
            mutableUiState.update {
                it.copy(
                    isUnlinkingSite = true,
                    unlinkingTeamId = teamId,
                    hasSelectionFailure = false,
                    hasUnlinkFailure = false,
                )
            }
            launchSessionRequest(expectedSession) {
                when (
                    unlinkAppTeamUseCase(
                        teamId,
                        siteId,
                        expectedSession.generation,
                    )
                ) {
                    is ApiResult.Success -> {
                        if (!isCurrentSession(expectedSession)) {
                            return@launchSessionRequest
                        }
                        mutableUiState.update {
                            it.copy(
                                isUnlinkingSite = false,
                                unlinkingTeamId = null,
                                linkedSites = it.linkedSites.filterNot { site -> site.teamId == teamId },
                                selectedTeamId = it.selectedTeamId.takeUnless { selectedId -> selectedId == teamId },
                                selectedSiteId = getSelectedAppSiteIdUseCase(),
                                openedSiteId = it.openedSiteId?.takeUnless { openedId -> openedId == siteId },
                                dashboard = null,
                                isLoading = true,
                                hasFailure = false,
                                hasUnlinkFailure = false,
                            )
                        }
                        loadLinkedSites(expectedSession)
                    }

                    is ApiResult.Failure -> {
                        if (isCurrentSession(expectedSession)) {
                            mutableUiState.update {
                                it.copy(
                                    isUnlinkingSite = false,
                                    unlinkingTeamId = null,
                                    isLoading = true,
                                    hasFailure = false,
                                    hasUnlinkFailure = false,
                                    dashboard = null,
                                )
                            }
                            val isListRefreshSuccessful = loadLinkedSites(expectedSession)
                            if (isCurrentSession(expectedSession)) {
                                val hasUnlinkedTeamInServerList =
                                    mutableUiState.value.linkedSites.any { site ->
                                        site.teamId == teamId
                                    }
                                mutableUiState.update {
                                    it.copy(
                                        hasUnlinkFailure =
                                            !isListRefreshSuccessful || hasUnlinkedTeamInServerList,
                                    )
                                }
                            }
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
                    owner.snapshot.role == SessionRole.TEAM_MEMBER &&
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
                expectedSession.role == SessionRole.TEAM_MEMBER
    }

private data class SessionOwner(
    val state: SessionState,
    val snapshot: SessionSnapshot,
)

internal data class AppAccountSitesUiState(
    val linkedSites: List<AppLinkedSite> = emptyList(),
    val selectedTeamId: String? = null,
    val selectedSiteId: String? = null,
    val isLoading: Boolean = false,
    val isSelectingSite: Boolean = false,
    val selectingTeamId: String? = null,
    val hasSelectionFailure: Boolean = false,
    val isUnlinkingSite: Boolean = false,
    val unlinkingTeamId: String? = null,
    val hasUnlinkFailure: Boolean = false,
    val openedSiteId: String? = null,
    val dashboard: AppSiteDashboard? = null,
    val isLoadingDashboard: Boolean = false,
    val hasDashboardFailure: Boolean = false,
    val hasFailure: Boolean = false,
)
