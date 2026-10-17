package com.nativelap.heartguard.viewmodel.profile

import androidx.compose.runtime.Immutable

/** 내 정보 수정 화면 상태다. 조회 결과([loadState])와 이름 입력·저장 진행 상태를 함께 가진다. */
@Immutable
data class ProfileEditUiState(
    val loadState: ProfileLoadState = ProfileLoadState.Loading,
    val nameInput: String = "",
    val isSaving: Boolean = false,
    val saveError: ProfileSaveError? = null,
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
        val companyName: String? = null,
        val version: Long? = null,
    ) : ProfileLoadState

    data object Failed : ProfileLoadState
}

/** 이름 저장 실패 이유다. */
enum class ProfileSaveError {
    // 네트워크·서버 오류로 저장하지 못했다.
    FAILURE,

    // 다른 곳에서 먼저 정보가 바뀌어 저장하지 못했다. 최신 정보를 다시 불러온 상태다.
    CONFLICT,
}
