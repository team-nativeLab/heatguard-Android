package com.nativelap.heartguard.viewmodel.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.clearStateWhenSessionEnds
import com.nativelap.heartguard.domain.profile.model.ProfileUpdateResult
import com.nativelap.heartguard.domain.profile.model.WorkerProfile
import com.nativelap.heartguard.domain.profile.usecase.GetWorkerProfileUseCase
import com.nativelap.heartguard.domain.profile.usecase.GetWorkerProfilePhotoUseCase
import com.nativelap.heartguard.domain.profile.usecase.UploadWorkerProfilePhotoUseCase
import com.nativelap.heartguard.domain.profile.usecase.DeleteWorkerProfilePhotoUseCase
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
        private val getWorkerProfilePhotoUseCase: GetWorkerProfilePhotoUseCase,
        private val updateWorkerNameUseCase: UpdateWorkerNameUseCase,
        private val uploadWorkerProfilePhotoUseCase: UploadWorkerProfilePhotoUseCase,
        private val deleteWorkerProfilePhotoUseCase: DeleteWorkerProfilePhotoUseCase,
        private val sessionManager: SessionManager,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ProfileEditUiState())
        val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

        private val effectChannel = Channel<ProfileEditEffect>(Channel.BUFFERED)
        val effects: Flow<ProfileEditEffect> = effectChannel.receiveAsFlow()

        private var loadProfileJob: Job? = null
        private var saveProfileJob: Job? = null
        private var profileSessionGeneration = sessionManager.getSnapshot().generation

        init {
            clearStateWhenSessionEnds(sessionManager) {
                clearState()
            }
        }

        /** 화면에 들어올 때와 재시도할 때 호출한다. 이전 입력은 버리고 서버 값으로 다시 채운다.
         * 이 ViewModel은 Activity 수명이라 화면 재진입 시 이전 입력이 남지 않도록 매번 새로 조회한다. */
        fun loadProfile() {
            val currentSessionGeneration = sessionManager.getSnapshot().generation
            if (profileSessionGeneration != currentSessionGeneration) {
                _uiState.value = ProfileEditUiState()
                profileSessionGeneration = currentSessionGeneration
            }
            loadProfileJob?.cancel()
            saveProfileJob?.cancel()
            _uiState.value = ProfileEditUiState(profilePhotoUri = _uiState.value.profilePhotoUri)
            val expectedSessionGeneration = profileSessionGeneration
            loadProfileJob =
                viewModelScope.launch {
                    when (val profileResult = getWorkerProfileUseCase(expectedSessionGeneration)) {
                        is ApiResult.Success -> {
                            if (!applyLoadedProfile(profileResult.value, expectedSessionGeneration)) {
                                return@launch
                            }
                            when (val photoResult = getWorkerProfilePhotoUseCase(expectedSessionGeneration)) {
                                is ApiResult.Success -> {
                                    if (!isCurrentProfileSession(expectedSessionGeneration)) {
                                        clearStateIfSessionChanged(expectedSessionGeneration)
                                        return@launch
                                    }
                                    _uiState.value =
                                        _uiState.value.copy(
                                            profilePhotoUrl = photoResult.value?.photoUrl,
                                        )
                                    if (!isCurrentProfileSession(expectedSessionGeneration)) {
                                        clearStateIfSessionChanged(expectedSessionGeneration)
                                    }
                                }

                                is ApiResult.Failure -> {
                                    if (!isCurrentProfileSession(expectedSessionGeneration)) {
                                        clearStateIfSessionChanged(expectedSessionGeneration)
                                        return@launch
                                    }
                                    _uiState.value =
                                        _uiState.value.copy(
                                            profilePhotoSaveError = ProfilePhotoSaveError.LOAD_FAILED,
                                        )
                                    if (!isCurrentProfileSession(expectedSessionGeneration)) {
                                        clearStateIfSessionChanged(expectedSessionGeneration)
                                    }
                                }
                            }
                        }

                        is ApiResult.Failure -> {
                            if (!isCurrentProfileSession(expectedSessionGeneration)) {
                                clearStateIfSessionChanged(expectedSessionGeneration)
                                return@launch
                            }
                            _uiState.value =
                                _uiState.value.copy(
                                    loadState = ProfileLoadState.Failed,
                                )
                            if (!isCurrentProfileSession(expectedSessionGeneration)) {
                                clearStateIfSessionChanged(expectedSessionGeneration)
                            }
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

        fun updateProfilePhoto(uri: Uri) {
            _uiState.value =
                _uiState.value.copy(
                    profilePhotoUri = uri,
                    shouldDeleteProfilePhoto = false,
                    profilePhotoSaveError = null,
                )
        }

        fun clearProfilePhoto() {
            _uiState.value =
                _uiState.value.copy(
                    profilePhotoUri = null,
                    shouldDeleteProfilePhoto = false,
                )
        }

        fun setPhotoActionsMenuExpanded(isExpanded: Boolean) {
            _uiState.value = _uiState.value.copy(isPhotoActionsMenuExpanded = isExpanded)
        }

        fun requestProfilePhotoDeletion() {
            val currentState = _uiState.value
            if (currentState.profilePhotoUrl.isNullOrBlank()) {
                return
            }
            _uiState.value =
                currentState.copy(
                    profilePhotoUri = null,
                    shouldDeleteProfilePhoto = true,
                    isPhotoActionsMenuExpanded = false,
                    profilePhotoSaveError = null,
                )
        }

        /** 입력한 이름을 저장한다. 성공하면 서버가 돌려준 값으로 화면을 갱신하고 [ProfileEditEffect.Saved]를 한 번 보낸다. */
        fun saveProfile() {
            if (profileSessionGeneration != sessionManager.getSnapshot().generation) {
                clearState()
                return
            }
            val currentState = _uiState.value
            if (!currentState.canSave) {
                return
            }

            val loadedProfile = currentState.loadState as? ProfileLoadState.Loaded ?: return
            val expectedSessionGeneration = profileSessionGeneration
            _uiState.value =
                currentState.copy(
                    isSaving = true,
                    saveError = null,
                    profilePhotoSaveError = null,
                )
            saveProfileJob =
                viewModelScope.launch {
                    val shouldUpdateName = currentState.nameInput.trim() != loadedProfile.userName.orEmpty()
                    if (shouldUpdateName) {
                        val nameUpdateResult =
                            updateWorkerNameUseCase(
                                name = currentState.nameInput,
                                version = loadedProfile.version,
                                expectedSessionGeneration = expectedSessionGeneration,
                            )
                        when (nameUpdateResult) {
                            is ProfileUpdateResult.Success -> {
                                if (!applyLoadedProfile(nameUpdateResult.profile, expectedSessionGeneration)) {
                                    return@launch
                                }
                            }

                            ProfileUpdateResult.Conflict -> {
                                if (!isCurrentProfileSession(expectedSessionGeneration)) {
                                    clearStateIfSessionChanged(expectedSessionGeneration)
                                    return@launch
                                }
                                reloadProfileKeepingInput(expectedSessionGeneration)
                                return@launch
                            }

                            ProfileUpdateResult.Failure -> {
                                if (!isCurrentProfileSession(expectedSessionGeneration)) {
                                    clearStateIfSessionChanged(expectedSessionGeneration)
                                    return@launch
                                }
                                _uiState.value =
                                    _uiState.value.copy(
                                        isSaving = false,
                                        saveError = ProfileSaveError.FAILURE,
                                    )
                                if (!isCurrentProfileSession(expectedSessionGeneration)) {
                                    clearStateIfSessionChanged(expectedSessionGeneration)
                                }
                                return@launch
                            }
                        }
                    }

                    when {
                        currentState.profilePhotoUri != null -> {
                            when (
                                val photoResult =
                                    uploadWorkerProfilePhotoUseCase(
                                        photoUri = currentState.profilePhotoUri.toString(),
                                        expectedSessionGeneration = expectedSessionGeneration,
                                    )
                            ) {
                                is ApiResult.Success -> {
                                    if (!isCurrentProfileSession(expectedSessionGeneration)) {
                                        clearStateIfSessionChanged(expectedSessionGeneration)
                                        return@launch
                                    }
                                    _uiState.value =
                                        _uiState.value.copy(
                                            profilePhotoUrl = photoResult.value.photoUrl,
                                            profilePhotoUri = null,
                                            shouldDeleteProfilePhoto = false,
                                            profilePhotoSaveError = null,
                                        )
                                    if (!isCurrentProfileSession(expectedSessionGeneration)) {
                                        clearStateIfSessionChanged(expectedSessionGeneration)
                                        return@launch
                                    }
                                }

                                is ApiResult.Failure -> {
                                    if (!isCurrentProfileSession(expectedSessionGeneration)) {
                                        clearStateIfSessionChanged(expectedSessionGeneration)
                                        return@launch
                                    }
                                    _uiState.value =
                                        _uiState.value.copy(
                                            isSaving = false,
                                            profilePhotoSaveError = ProfilePhotoSaveError.UPLOAD_FAILED,
                                        )
                                    if (!isCurrentProfileSession(expectedSessionGeneration)) {
                                        clearStateIfSessionChanged(expectedSessionGeneration)
                                    }
                                    return@launch
                                }
                            }
                        }

                        currentState.shouldDeleteProfilePhoto -> {
                            when (deleteWorkerProfilePhotoUseCase(expectedSessionGeneration)) {
                                is ApiResult.Success -> {
                                    if (!isCurrentProfileSession(expectedSessionGeneration)) {
                                        clearStateIfSessionChanged(expectedSessionGeneration)
                                        return@launch
                                    }
                                    _uiState.value =
                                        _uiState.value.copy(
                                            profilePhotoUrl = null,
                                            profilePhotoUri = null,
                                            shouldDeleteProfilePhoto = false,
                                            profilePhotoSaveError = null,
                                        )
                                    if (!isCurrentProfileSession(expectedSessionGeneration)) {
                                        clearStateIfSessionChanged(expectedSessionGeneration)
                                        return@launch
                                    }
                                }

                                is ApiResult.Failure -> {
                                    if (!isCurrentProfileSession(expectedSessionGeneration)) {
                                        clearStateIfSessionChanged(expectedSessionGeneration)
                                        return@launch
                                    }
                                    _uiState.value =
                                        _uiState.value.copy(
                                            isSaving = false,
                                            profilePhotoSaveError = ProfilePhotoSaveError.DELETE_FAILED,
                                        )
                                    if (!isCurrentProfileSession(expectedSessionGeneration)) {
                                        clearStateIfSessionChanged(expectedSessionGeneration)
                                    }
                                    return@launch
                                }
                            }
                        }
                    }

                    if (!isCurrentProfileSession(expectedSessionGeneration)) {
                        clearStateIfSessionChanged(expectedSessionGeneration)
                        return@launch
                    }
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    if (!isCurrentProfileSession(expectedSessionGeneration)) {
                        clearStateIfSessionChanged(expectedSessionGeneration)
                        return@launch
                    }
                    effectChannel.send(ProfileEditEffect.Saved)
                }
        }

        // 저장 충돌 뒤 최신 정보(버전)를 다시 받아오되, 사용자가 입력한 이름은 그대로 둔다.
        private suspend fun reloadProfileKeepingInput(expectedSessionGeneration: Long) {
            val profileResult = getWorkerProfileUseCase(expectedSessionGeneration)
            if (!isCurrentProfileSession(expectedSessionGeneration)) {
                clearStateIfSessionChanged(expectedSessionGeneration)
                return
            }
            val reloadedLoadState =
                when (profileResult) {
                    is ApiResult.Success -> profileResult.value.toLoadedState()
                    is ApiResult.Failure -> _uiState.value.loadState
                }
            _uiState.value =
                _uiState.value.copy(
                    loadState = reloadedLoadState,
                    profilePhotoUri = _uiState.value.profilePhotoUri,
                    isSaving = false,
                    saveError = ProfileSaveError.CONFLICT,
                )
            if (!isCurrentProfileSession(expectedSessionGeneration)) {
                clearStateIfSessionChanged(expectedSessionGeneration)
            }
        }

        private fun applyLoadedProfile(
            workerProfile: WorkerProfile,
            expectedSessionGeneration: Long,
        ): Boolean {
            if (!isCurrentProfileSession(expectedSessionGeneration)) {
                clearStateIfSessionChanged(expectedSessionGeneration)
                return false
            }
            _uiState.value =
                ProfileEditUiState(
                    loadState = workerProfile.toLoadedState(),
                    nameInput = workerProfile.name.orEmpty(),
                    profilePhotoUri = _uiState.value.profilePhotoUri,
                    profilePhotoUrl = _uiState.value.profilePhotoUrl,
                    shouldDeleteProfilePhoto = _uiState.value.shouldDeleteProfilePhoto,
                    profilePhotoSaveError = _uiState.value.profilePhotoSaveError,
                    isSaving = _uiState.value.isSaving,
                )
            if (!isCurrentProfileSession(expectedSessionGeneration)) {
                clearStateIfSessionChanged(expectedSessionGeneration)
                return false
            }
            return true
        }

        private fun isCurrentProfileSession(expectedSessionGeneration: Long): Boolean =
            profileSessionGeneration == expectedSessionGeneration &&
                sessionManager.getSnapshot().generation == expectedSessionGeneration

        private fun clearStateIfSessionChanged(expectedSessionGeneration: Long) {
            if (profileSessionGeneration == expectedSessionGeneration &&
                sessionManager.getSnapshot().generation != expectedSessionGeneration
            ) {
                clearState()
            }
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
            profileSessionGeneration = sessionManager.getSnapshot().generation
        }
    }
