package com.nativelap.heartguard.data.site.mapper

import com.nativelap.heartguard.data.site.dto.AppLinkedSiteDto
import com.nativelap.heartguard.data.site.dto.AppLinkedSitesResponseDto
import com.nativelap.heartguard.data.site.dto.AppSiteLinkClaimResponseDto
import com.nativelap.heartguard.data.site.dto.AppSiteSelectionResponseDto
import com.nativelap.heartguard.domain.site.model.AppLinkedSite
import com.nativelap.heartguard.domain.site.model.AppLinkedSites
import com.nativelap.heartguard.domain.site.model.AppSelectedSite
import com.nativelap.heartguard.domain.site.model.AppSiteLinkClaim

fun AppLinkedSiteDto.toDomain(): AppLinkedSite =
    AppLinkedSite(
        teamId = teamId,
        teamName = teamName,
        siteId = siteId,
        siteName = siteName,
        companyId = companyId,
    )

fun AppLinkedSitesResponseDto.toDomain(): AppLinkedSites =
    AppLinkedSites(
        items = items.map(AppLinkedSiteDto::toDomain),
        selectedTeamId = selectedTeamId,
    )

fun AppSiteLinkClaimResponseDto.toDomain(): AppSiteLinkClaim =
    AppSiteLinkClaim(
        siteId = siteId,
        siteName = siteName,
        linkedAt = linkedAt,
    )

fun AppSiteSelectionResponseDto.toDomain(): AppSelectedSite =
    AppSelectedSite(
        teamId = teamId,
        teamName = teamName,
        siteId = siteId,
    )
