package com.nativelap.heartguard.viewmodel.notification

sealed interface TeamNotificationViewEffect {
    data object ReadFailed : TeamNotificationViewEffect
    data object ReadAllFailed : TeamNotificationViewEffect
    data object RefreshFailed : TeamNotificationViewEffect
}
