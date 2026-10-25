package com.nativelap.heartguard.data.site.mapper

import com.nativelap.heartguard.data.site.dto.AppLinkedSiteDto
import com.nativelap.heartguard.data.site.dto.AppSiteLinkClaimResponseDto
import com.nativelap.heartguard.data.site.dto.AppSiteSelectionResponseDto
import com.nativelap.heartguard.domain.site.model.AppLinkedSite
import com.nativelap.heartguard.domain.site.model.AppSelectedSite
import com.nativelap.heartguard.domain.site.model.AppSiteLinkClaim

fun AppLinkedSiteDto.toDomain(): AppLinkedSite =
    AppLinkedSite(
        siteId = siteId,
        siteName = siteName,
        managerName = managerName,
        linkedAt = linkedAt,
    )

fun AppSiteLinkClaimResponseDto.toDomain(): AppSiteLinkClaim =
    AppSiteLinkClaim(
        siteId = siteId,
        siteName = siteName,
        linkedAt = linkedAt,
    )

fun AppSiteSelectionResponseDto.toDomain(): AppSelectedSite =
    AppSelectedSite(
        siteId = siteId,
        siteName = siteName,
    )
