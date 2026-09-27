package com.nativelap.heartguard.viewmodel.menu

/** 메뉴 드로어에서 발생하는 사용자 의도를 Route 한 곳으로 모아 전달하기 위한 이벤트다. */
sealed interface MenuDrawerEvent {
    data object EditProfileClicked : MenuDrawerEvent

    data object NotificationSettingsClicked : MenuDrawerEvent

    data object NoticesClicked : MenuDrawerEvent

    data object CustomerCenterClicked : MenuDrawerEvent

    data object LogoutClicked : MenuDrawerEvent

    data object WithdrawClicked : MenuDrawerEvent
}
