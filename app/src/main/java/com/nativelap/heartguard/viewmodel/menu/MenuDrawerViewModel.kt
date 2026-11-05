package com.nativelap.heartguard.viewmodel.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.clearStateWhenSessionEnds
import com.nativelap.heartguard.core.util.ProfilePhotoMemoryCache
import com.nativelap.heartguard.domain.auth.usecase.TeamLogoutUseCase
import com.nativelap.heartguard.domain.profile.usecase.ClearWorkerProfilePhotoCacheUseCase
import com.nativelap.heartguard.domain.profile.usecase.GetWorkerProfilePhotoUseCase
import com.nativelap.heartguard.domain.profile.usecase.GetWorkerProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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
        private val profilePhotoMemoryCache: ProfilePhotoMemoryCache,
        private val clearWorkerProfilePhotoCacheUseCase: ClearWorkerProfilePhotoCacheUseCase,
    ) : ViewModel() {
        private val _profile = MutableStateFlow(MenuDrawerProfileUiModel())
        val profile: StateFlow<MenuDrawerProfileUiModel> = _profile.asStateFlow()

        private val logoutBlockedChannel = Channel<Unit>(Channel.BUFFERED)

        /** 진행 중 긴급 호출 종료가 확인되지 않아 로그아웃이 막혔을 때 한 번 보낸다. */
        val logoutBlocked: Flow<Unit> = logoutBlockedChannel.receiveAsFlow()

        private val _isLoggingOut = MutableStateFlow(false)

        /** 로그아웃(긴급 호출 종료 포함)이 진행 중인지 드로어가 보여 준다. */
        val isLoggingOut: StateFlow<Boolean> = _isLoggingOut.asStateFlow()
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
                profilePhotoMemoryCache.clearSession(profileSessionGeneration)
                clearWorkerProfilePhotoCacheUseCase(profileSessionGeneration)
                _profile.value = MenuDrawerProfileUiModel()
                profileSessionGeneration = expectedSessionGeneration
            }
            val expectedPhotoRevision =
                profilePhotoMemoryCache.revisionFor(expectedSessionGeneration)
            val currentPhotoUrl = _profile.value.photoUrl
            if (currentPhotoUrl != null &&
                !profilePhotoMemoryCache.isCurrentKey(
                    cacheKey = _profile.value.photoMemoryCacheKey,
                    sessionGeneration = expectedSessionGeneration,
                )
            ) {
                _profile.value =
                    _profile.value.copy(
                        photoUrl = null,
                        photoMemoryCacheKey = null,
                    )
            }
            loadProfileJob?.cancel()
            loadProfileJob =
                viewModelScope.launch {
                    coroutineScope {
                        launch {
                            val profileResult = getWorkerProfileUseCase(expectedSessionGeneration)
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
                                }
                            }
                        }
                        launch {
                            val photoResult = getWorkerProfilePhotoUseCase(expectedSessionGeneration)
                            if (!isCurrentProfileSession(expectedSessionGeneration)) {
                                clearProfileIfSessionChanged(expectedSessionGeneration)
                                return@launch
                            }
                            if (profilePhotoMemoryCache.isCurrentRevision(
                                    expectedRevision = expectedPhotoRevision,
                                    sessionGeneration = expectedSessionGeneration,
                                )
                            ) {
                                when (photoResult) {
                                    is ApiResult.Success -> {
                                        val photoCacheKey =
                                            profilePhotoMemoryCache.keyFor(
                                                photoUrl = photoResult.value?.photoUrl,
                                                sessionGeneration = expectedSessionGeneration,
                                                imageVariant = MENU_PROFILE_IMAGE_VARIANT,
                                            )
                                        _profile.value =
                                            _profile.value.copy(
                                                photoUrl = photoResult.value?.photoUrl,
                                                photoMemoryCacheKey = photoCacheKey,
                                            )
                                    }

                                    is ApiResult.Failure -> {
                                        _profile.value =
                                            _profile.value.copy(
                                                photoUrl = null,
                                                photoMemoryCacheKey = null,
                                            )
                                    }
                                }
                            }
                            if (!isCurrentProfileSession(expectedSessionGeneration)) {
                                clearProfileIfSessionChanged(expectedSessionGeneration)
                            }
                        }
                    }
                }
        }

        /** 로그아웃 시 진행 중 긴급 호출을 먼저 끝내고 서버 세션을 폐기한다. 요청이 실패해도 로컬 세션은 종료한다.
         * 긴급 호출 종료가 확인되지 않으면 로그아웃을 막고 [logoutBlocked]를 보낸다. */
        fun logout() {
            if (_isLoggingOut.value) {
                return
            }

            _isLoggingOut.value = true
            val logoutGeneration = sessionManager.getSnapshot().generation
            viewModelScope.launch {
                var shouldExpireSession = true
                try {
                    val loggedOut = teamLogoutUseCase()
                    if (!loggedOut) {
                        shouldExpireSession = false
                        // 요청 중 세션이 이미 만료됐다면(401 등) 로그아웃은 끝난 것이므로 차단 안내를 띄우지 않는다.
                        if (sessionManager.getSnapshot().generation == logoutGeneration) {
                            logoutBlockedChannel.trySend(Unit)
                        }
                    }
                } finally {
                    try {
                        if (shouldExpireSession) {
                            withContext(NonCancellable) {
                                sessionManager.expireSession(logoutGeneration)
                            }
                        }
                    } finally {
                        _isLoggingOut.value = false
                    }
                }
            }
        }

        // 세션이 끝나면 진행 중인 조회를 멈추고 이전 작업자의 표시 값을 지운다.
        private fun clearProfile() {
            loadProfileJob?.cancel()
            loadProfileJob = null
            profilePhotoMemoryCache.clearSession(profileSessionGeneration)
            clearWorkerProfilePhotoCacheUseCase(profileSessionGeneration)
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

        private companion object {
            const val MENU_PROFILE_IMAGE_VARIANT = "menu"
        }
    }

internal fun isProfileSessionCurrent(
    expectedSessionGeneration: Long,
    ownedSessionGeneration: Long,
    currentSessionGeneration: Long,
): Boolean =
    expectedSessionGeneration == ownedSessionGeneration &&
        expectedSessionGeneration == currentSessionGeneration
