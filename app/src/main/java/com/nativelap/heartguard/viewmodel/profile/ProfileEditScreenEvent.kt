package com.nativelap.heartguard.viewmodel.profile

/** 프로필 화면에서 발생하는 사용자 의도를 Route에 전달한다. */
sealed interface ProfileEditScreenEvent {
    data object BackClicked : ProfileEditScreenEvent
}
