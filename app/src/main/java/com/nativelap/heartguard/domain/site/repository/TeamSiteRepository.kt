package com.nativelap.heartguard.domain.site.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.site.model.TeamSiteOverview

/** 팀 현장페이지(홈) 조회를 도메인 모델로 제공한다. DTO·Retrofit 타입은 노출하지 않는다. */
interface TeamSiteRepository {
    suspend fun getTeamSiteOverview(): ApiResult<TeamSiteOverview>
}
