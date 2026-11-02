package com.nativelap.heartguard.viewmodel.profile

/** 프로필 화면에서 발생하는 사용자 의도를 Route에 전달한다. */
sealed interface ProfileEditScreenEvent {
    data object BackClicked : ProfileEditScreenEvent

    data class NameChanged(
        val name: String,
    ) : ProfileEditScreenEvent

    data object SaveClicked : ProfileEditScreenEvent

    data object RetryClicked : ProfileEditScreenEvent

    data object PasswordChangeClicked : ProfileEditScreenEvent

    // 아바타 또는 "사진을 눌러..." 문구를 눌러 프로필 사진 변경 시트를 연다.
    data object ProfilePhotoClicked : ProfileEditScreenEvent

    data object ProfilePhotoSheetDismissed : ProfileEditScreenEvent

    data object ProfilePhotoCaptureSelected : ProfileEditScreenEvent

    data object ProfilePhotoChangeSelected : ProfileEditScreenEvent

    data object ProfilePhotoDeleteSelected : ProfileEditScreenEvent
}
