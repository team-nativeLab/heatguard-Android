package com.nativelap.heartguard.viewmodel.menu

import androidx.compose.runtime.Immutable

/** 메뉴 드로어 상단 프로필 영역에 표시할 사용자 정보다. */
@Immutable
data class MenuDrawerProfileUiModel(
    val userName: String,
    val companyName: String,
    val jobTitle: String,
    val email: String,
) {
    // 아바타 원 안에 보여줄 이름 첫 글자이며, 이름이 비어 있으면 빈 문자열이다.
    val avatarInitial: String
        get() = userName.take(1)
}
