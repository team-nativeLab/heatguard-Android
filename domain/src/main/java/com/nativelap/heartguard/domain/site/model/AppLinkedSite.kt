package com.nativelap.heartguard.domain.site.model

data class AppLinkedSite(
    val siteId: String,
    val siteName: String,
    val managerName: String?,
    val linkedAt: String,
)
