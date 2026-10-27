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

    data object ProfilePhotoClicked : ProfileEditScreenEvent

    data object ProfilePhotoMenuDismissed : ProfileEditScreenEvent

    data object ProfilePhotoChangeSelected : ProfileEditScreenEvent

    data object ProfilePhotoDeleteSelected : ProfileEditScreenEvent

    data object ProfilePhotoDeleteConfirmed : ProfileEditScreenEvent

    data object ProfilePhotoDeleteCancelled : ProfileEditScreenEvent
}
