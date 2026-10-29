package com.nativelap.heartguard.view.screen.site

internal sealed interface AppAccountSitesScreenEvent {
    data object BackClicked : AppAccountSitesScreenEvent

    data object RetryClicked : AppAccountSitesScreenEvent

    data class TeamClicked(
        val teamId: String,
        val siteId: String,
    ) : AppAccountSitesScreenEvent
}
