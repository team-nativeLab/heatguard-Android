package com.nativelap.heartguard.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.clearStateWhenSessionEnds
import com.nativelap.heartguard.domain.profile.model.ProfileUpdateResult
import com.nativelap.heartguard.domain.profile.model.WorkerProfile
import com.nativelap.heartguard.domain.profile.usecase.GetWorkerProfileUseCase
import com.nativelap.heartguard.domain.profile.usecase.UpdateWorkerNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 내 정보 수정 화면의 작업자 정보 조회와 이름 저장을 담당한다. */
@HiltViewModel
class ProfileEditViewModel
    @Inject
    constructor(
        private val getWorkerProfileUseCase: GetWorkerProfileUseCase,
        private val updateWorkerNameUseCase: UpdateWorkerNameUseCase,
        sessionManager: SessionManager,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ProfileEditUiState())
        val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

        private val effectChannel = Channel<ProfileEditEffect>(Channel.BUFFERED)
        val effects: Flow<ProfileEditEffect> = effectChannel.receiveAsFlow()

        private var loadProfileJob: Job? = null
        private var saveProfileJob: Job? = null

        init {
            clearStateWhenSessionEnds(sessionManager) {
                clearState()
            }
        }

        /** 화면에 들어올 때와 재시도할 때 호출한다. 이전 입력은 버리고 서버 값으로 다시 채운다.
         * 이 ViewModel은 Activity 수명이라 화면 재진입 시 이전 입력이 남지 않도록 매번 새로 조회한다. */
        fun loadProfile() {
            loadProfileJob?.cancel()
            saveProfileJob?.cancel()
            _uiState.value = ProfileEditUiState()
            loadProfileJob =
                viewModelScope.launch {
                    when (val profileResult = getWorkerProfileUseCase()) {
                        is ApiResult.Success -> {
                            applyLoadedProfile(profileResult.value)
                        }

                        is ApiResult.Failure -> {
                            _uiState.value =
                                _uiState.value.copy(
                                    loadState = ProfileLoadState.Failed,
                                )
                        }
                    }
                }
        }

        /** 이름 입력을 반영하고 이전 저장 실패 안내를 지운다. */
        fun updateName(name: String) {
            _uiState.value =
                _uiState.value.copy(
                    nameInput = name,
                    saveError = null,
                )
        }

        /** 입력한 이름을 저장한다. 성공하면 서버가 돌려준 값으로 화면을 갱신하고 [ProfileEditEffect.Saved]를 한 번 보낸다. */
        fun saveProfile() {
            val currentState = _uiState.value
            if (!currentState.canSave) {
                return
            }

            val loadedVersion = (currentState.loadState as? ProfileLoadState.Loaded)?.version
            _uiState.value =
                currentState.copy(
                    isSaving = true,
                    saveError = null,
                )
            saveProfileJob =
                viewModelScope.launch {
                    val saveResult =
                        updateWorkerNameUseCase(
                            name = currentState.nameInput,
                            version = loadedVersion,
                        )
                    when (saveResult) {
                        is ProfileUpdateResult.Success -> {
                            applyLoadedProfile(saveResult.profile)
                            effectChannel.send(ProfileEditEffect.Saved)
                        }

                        ProfileUpdateResult.Conflict -> {
                            reloadProfileKeepingInput()
                        }

                        ProfileUpdateResult.Failure -> {
                            _uiState.value =
                                _uiState.value.copy(
                                    isSaving = false,
                                    saveError = ProfileSaveError.FAILURE,
                                )
                        }
                    }
                }
        }

        // 저장 충돌 뒤 최신 정보(버전)를 다시 받아오되, 사용자가 입력한 이름은 그대로 둔다.
        private suspend fun reloadProfileKeepingInput() {
            val profileResult = getWorkerProfileUseCase()
            val reloadedLoadState =
                when (profileResult) {
                    is ApiResult.Success -> profileResult.value.toLoadedState()
                    is ApiResult.Failure -> _uiState.value.loadState
                }
            _uiState.value =
                _uiState.value.copy(
                    loadState = reloadedLoadState,
                    isSaving = false,
                    saveError = ProfileSaveError.CONFLICT,
                )
        }

        private fun applyLoadedProfile(workerProfile: WorkerProfile) {
            _uiState.value =
                ProfileEditUiState(
                    loadState = workerProfile.toLoadedState(),
                    nameInput = workerProfile.name.orEmpty(),
                )
        }

        private fun WorkerProfile.toLoadedState(): ProfileLoadState.Loaded =
            ProfileLoadState.Loaded(
                userName = name,
                email = email,
                companyName = companyName,
                version = version,
            )

        // 세션이 끝나면 진행 중인 요청을 멈추고 이전 작업자의 정보와 입력값을 지운다.
        private fun clearState() {
            loadProfileJob?.cancel()
            saveProfileJob?.cancel()
            loadProfileJob = null
            saveProfileJob = null
            _uiState.value = ProfileEditUiState()
        }
    }
