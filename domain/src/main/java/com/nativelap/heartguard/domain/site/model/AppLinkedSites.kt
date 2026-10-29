package com.nativelap.heartguard.domain.site.model

data class AppLinkedSites(
    val items: List<AppLinkedSite>,
    val selectedTeamId: String?,
)
