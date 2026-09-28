package com.nativelap.heartguard.viewmodel.profile

import androidx.compose.runtime.Immutable

/** 내 정보 수정 화면 상태다. 조회 결과([loadState])와 이름 입력·저장 진행 상태를 함께 가진다. */
@Immutable
data class ProfileEditUiState(
    val loadState: ProfileLoadState = ProfileLoadState.Loading,
    val nameInput: String = "",
    val isSaving: Boolean = false,
    val hasSaveError: Boolean = false,
) {
    // 조회한 이름과 달라졌고 비어 있지 않을 때만 저장할 수 있다. 저장 중에는 중복 요청을 막는다.
    val canSave: Boolean
        get() {
            val loadedProfile = (loadState as? ProfileLoadState.Loaded) ?: return false
            val trimmedName = nameInput.trim()

            return !isSaving &&
                trimmedName.isNotEmpty() &&
                trimmedName != loadedProfile.userName.orEmpty()
        }
}

/** 작업자 정보 조회 결과다. */
sealed interface ProfileLoadState {
    data object Loading : ProfileLoadState

    data class Loaded(
        val userName: String?,
        val email: String?,
    ) : ProfileLoadState

    data object Failed : ProfileLoadState
}
