package com.nativelap.heartguard.viewmodel.password

/** 변경 완료처럼 화면이 한 번만 반응해야 하는 결과다. */
sealed interface PasswordChangeEffect {
    data object Changed : PasswordChangeEffect
}
