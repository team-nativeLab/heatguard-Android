package com.nativelap.heartguard.viewmodel.profile

import android.net.Uri
import androidx.compose.runtime.Immutable

/** 내 정보 수정 화면 상태다. 조회 결과([loadState])와 이름 입력·저장 진행 상태를 함께 가진다. */
@Immutable
data class ProfileEditUiState(
    val loadState: ProfileLoadState = ProfileLoadState.Loading,
    val nameInput: String = "",
    val profilePhotoUri: Uri? = null,
    val profilePhotoUrl: String? = null,
    val shouldDeleteProfilePhoto: Boolean = false,
    val profilePhotoSaveError: ProfilePhotoSaveError? = null,
    val isSaving: Boolean = false,
    val saveError: ProfileSaveError? = null,
) {
    // 조회한 이름과 달라졌고 비어 있지 않을 때만 저장할 수 있다. 저장 중에는 중복 요청을 막는다.
    val canSave: Boolean
        get() {
            val loadedProfile = (loadState as? ProfileLoadState.Loaded) ?: return false
            val trimmedName = nameInput.trim()

            val nameChanged = trimmedName.isNotEmpty() && trimmedName != loadedProfile.userName.orEmpty()
            val photoChanged = profilePhotoUri != null || shouldDeleteProfilePhoto

            return !isSaving && (nameChanged || photoChanged)
        }

    // 저장 중에는 사진을 고르거나 지우지 않는다. 업로드가 촬영 파일을 읽는 중일 수 있다.
    val canChangeProfilePhoto: Boolean
        get() = !isSaving

    // "기본 이미지로 변경"을 눌렀을 때 할 동작이다. 서버 사진 여부와 미저장 선택 사진 여부로 정한다.
    val defaultPhotoAction: ProfileDefaultPhotoAction
        get() =
            resolveProfilePhotoDefaultAction(
                hasServerPhoto = !profilePhotoUrl.isNullOrBlank(),
                hasSelectedPhoto = profilePhotoUri != null,
            )
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

enum class ProfilePhotoSaveError {
    LOAD_FAILED,
    UPLOAD_FAILED,
    DELETE_FAILED,

    // 카메라 촬영을 시작하지 못했거나 촬영 결과를 쓸 수 없었다.
    CAPTURE_FAILED,
}

/** "기본 이미지로 변경"을 눌렀을 때 화면이 해야 할 동작이다. */
enum class ProfileDefaultPhotoAction {
    // 서버 사진을 지울지 확인 다이얼로그를 연다.
    ConfirmDeletion,

    // 저장하지 않은 선택 사진만 취소한다. 서버에는 요청하지 않는다.
    ClearSelection,

    // 바꿀 사진이 없으므로 시트만 닫는다.
    None,
}

// 서버 사진이 있으면 삭제 확인을, 없고 미저장 선택 사진만 있으면 선택 취소를, 둘 다 없으면 아무 것도 하지 않는다.
internal fun resolveProfilePhotoDefaultAction(
    hasServerPhoto: Boolean,
    hasSelectedPhoto: Boolean,
): ProfileDefaultPhotoAction =
    when {
        hasServerPhoto -> ProfileDefaultPhotoAction.ConfirmDeletion
        hasSelectedPhoto -> ProfileDefaultPhotoAction.ClearSelection
        else -> ProfileDefaultPhotoAction.None
    }
