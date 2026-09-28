package com.nativelap.heartguard.viewmodel.profile

/** 저장 완료처럼 화면이 한 번만 반응해야 하는 결과다. UiState에 섞지 않고 Route가 수집한다. */
sealed interface ProfileEditEffect {
    data object Saved : ProfileEditEffect
}
