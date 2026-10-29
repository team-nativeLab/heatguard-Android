package com.nativelap.heartguard.domain.site.model

data class AppLinkedSite(
    val teamId: String,
    val teamName: String,
    val siteId: String,
    val siteName: String,
    val companyId: String?,
)
