package com.nativelap.heartguard.viewmodel.menu

import androidx.compose.runtime.Immutable

/** 메뉴 드로어 상단 프로필 영역에 표시할 사용자 정보다.
 * 이름·이메일은 작업자 정보 조회(GET /auth/team/me), 팀명·작업 위치는 홈 조회(GET /team) 값이다.
 * 서버에서 받지 못한 값은 null이며 화면은 "--"로 표시한다. */
@Immutable
data class MenuDrawerProfileUiModel(
    val userName: String? = null,
    val email: String? = null,
    val teamName: String? = null,
    val workplace: String? = null,
) {
    // 아바타 원 안에 보여줄 이름 첫 글자이며, 이름이 없으면 빈 문자열이다.
    val avatarInitial: String
        get() = userName?.take(1).orEmpty()
}
