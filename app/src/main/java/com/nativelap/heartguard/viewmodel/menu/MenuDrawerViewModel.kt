package com.nativelap.heartguard.viewmodel.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.clearStateWhenSessionEnds
import com.nativelap.heartguard.domain.auth.usecase.TeamLogoutUseCase
import com.nativelap.heartguard.domain.profile.usecase.GetWorkerProfilePhotoUseCase
import com.nativelap.heartguard.domain.profile.usecase.GetWorkerProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** 메뉴 드로어의 작업자 프로필 표시와 로그아웃을 담당한다. 팀명·작업 위치는 홈 조회 값을 Route가 합친다. */
@HiltViewModel
class MenuDrawerViewModel
    @Inject
    constructor(
        private val getWorkerProfileUseCase: GetWorkerProfileUseCase,
        private val getWorkerProfilePhotoUseCase: GetWorkerProfilePhotoUseCase,
        private val teamLogoutUseCase: TeamLogoutUseCase,
        private val sessionManager: SessionManager,
    ) : ViewModel() {
        private val _profile = MutableStateFlow(MenuDrawerProfileUiModel())
        val profile: StateFlow<MenuDrawerProfileUiModel> = _profile.asStateFlow()

        private var isLoggingOut = false
        private var loadProfileJob: Job? = null
        private var profileSessionGeneration = sessionManager.getSnapshot().generation

        init {
            clearStateWhenSessionEnds(sessionManager) {
                clearProfile()
            }
        }

        /** 드로어를 열 때 최신 작업자 정보와 프로필 사진 URL을 함께 조회한다. */
        fun loadProfile() {
            val expectedSessionGeneration = sessionManager.getSnapshot().generation
            if (profileSessionGeneration != expectedSessionGeneration) {
                _profile.value = MenuDrawerProfileUiModel()
                profileSessionGeneration = expectedSessionGeneration
            }
            loadProfileJob?.cancel()
            loadProfileJob =
                viewModelScope.launch {
                    val (profileResult, photoResult) =
                        coroutineScope {
                            val profileRequest =
                                async {
                                    getWorkerProfileUseCase(expectedSessionGeneration)
                                }
                            val photoRequest =
                                async {
                                    getWorkerProfilePhotoUseCase(expectedSessionGeneration)
                                }
                            profileRequest.await() to photoRequest.await()
                        }
                    if (!isCurrentProfileSession(expectedSessionGeneration)) {
                        clearProfileIfSessionChanged(expectedSessionGeneration)
                        return@launch
                    }
                    if (profileResult is ApiResult.Success) {
                        _profile.value =
                            _profile.value.copy(
                                userName = profileResult.value.name,
                                email = profileResult.value.email,
                                companyName = profileResult.value.companyName,
                            )
                        if (!isCurrentProfileSession(expectedSessionGeneration)) {
                            clearProfileIfSessionChanged(expectedSessionGeneration)
                            return@launch
                        }
                    }
                    if (photoResult is ApiResult.Success) {
                        if (!isCurrentProfileSession(expectedSessionGeneration)) {
                            clearProfileIfSessionChanged(expectedSessionGeneration)
                            return@launch
                        }
                        _profile.value =
                            _profile.value.copy(
                                photoUrl = photoResult.value?.photoUrl,
                            )
                        if (!isCurrentProfileSession(expectedSessionGeneration)) {
                            clearProfileIfSessionChanged(expectedSessionGeneration)
                        }
                    } else {
                        if (!isCurrentProfileSession(expectedSessionGeneration)) {
                            clearProfileIfSessionChanged(expectedSessionGeneration)
                            return@launch
                        }
                        _profile.value = _profile.value.copy(photoUrl = null)
                    }
                }
        }

        /** 로그아웃 시 서버 세션을 먼저 폐기하고, 요청이 실패하거나 취소돼도 로컬 세션을 종료한다. */
        fun logout() {
            if (isLoggingOut) {
                return
            }

            isLoggingOut = true
            val logoutGeneration = sessionManager.getSnapshot().generation
            viewModelScope.launch {
                try {
                    teamLogoutUseCase()
                } finally {
                    try {
                        withContext(NonCancellable) {
                            sessionManager.expireSession(logoutGeneration)
                        }
                    } finally {
                        isLoggingOut = false
                    }
                }
            }
        }

        // 세션이 끝나면 진행 중인 조회를 멈추고 이전 작업자의 표시 값을 지운다.
        private fun clearProfile() {
            loadProfileJob?.cancel()
            loadProfileJob = null
            _profile.value = MenuDrawerProfileUiModel()
            profileSessionGeneration = sessionManager.getSnapshot().generation
        }

        private fun isCurrentProfileSession(expectedSessionGeneration: Long): Boolean =
            isProfileSessionCurrent(
                expectedSessionGeneration = expectedSessionGeneration,
                ownedSessionGeneration = profileSessionGeneration,
                currentSessionGeneration = sessionManager.getSnapshot().generation,
            )

        private fun clearProfileIfSessionChanged(expectedSessionGeneration: Long) {
            if (profileSessionGeneration == expectedSessionGeneration &&
                sessionManager.getSnapshot().generation != expectedSessionGeneration
            ) {
                clearProfile()
            }
        }
    }

internal fun isProfileSessionCurrent(
    expectedSessionGeneration: Long,
    ownedSessionGeneration: Long,
    currentSessionGeneration: Long,
): Boolean =
    expectedSessionGeneration == ownedSessionGeneration &&
        expectedSessionGeneration == currentSessionGeneration
