package com.nativelap.heartguard.data.checklist.remote

import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.requireSuccessData
import com.nativelap.heartguard.data.checklist.dto.ChecklistItemStatusDto
import com.nativelap.heartguard.data.checklist.dto.ChecklistRoundItemStatusDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistResponseDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistRoundDetailResponseDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistRoundsResponseDto
import com.nativelap.heartguard.data.checklist.dto.UpdateChecklistItemRequestDto
import com.nativelap.heartguard.data.checklist.dto.UpdateChecklistRoundItemRequestDto
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

class TeamChecklistRemoteDataSourceImpl
    @Inject
    constructor(
        private val teamChecklistApiService: TeamChecklistApiService,
        private val apiExecutor: ApiExecutor,
    ) : TeamChecklistRemoteDataSource {
        override suspend fun getChecklist(): ApiResult<TeamChecklistResponseDto> =
            apiExecutor
                .execute { teamChecklistApiService.getChecklist() }
                .requireSuccessData()

        override suspend fun setItemChecked(
            itemId: String,
            request: UpdateChecklistItemRequestDto,
        ): ApiResult<ChecklistItemStatusDto> =
            apiExecutor
                .execute { teamChecklistApiService.setItemChecked(itemId, request) }
                .requireSuccessData()

        override suspend fun getRounds(date: String): ApiResult<TeamChecklistRoundsResponseDto> =
            apiExecutor
                .execute { teamChecklistApiService.getRounds(date) }
                .requireSuccessData()

        override suspend fun getRound(
            roundId: String,
            date: String,
        ): ApiResult<TeamChecklistRoundDetailResponseDto> =
            apiExecutor
                .execute { teamChecklistApiService.getRound(roundId.encodeChecklistPathSegment(), date) }
                .requireSuccessData()

        override suspend fun setRoundItemChecked(
            roundId: String,
            itemId: String,
            request: UpdateChecklistRoundItemRequestDto,
        ): ApiResult<ChecklistRoundItemStatusDto> =
            apiExecutor
                .execute {
                    teamChecklistApiService.setRoundItemChecked(
                        encodedRoundId = roundId.encodeChecklistPathSegment(),
                        encodedItemId = itemId.encodeChecklistPathSegment(),
                        request = request,
                    )
                }.requireSuccessData()
    }

internal fun String.encodeChecklistPathSegment(): String =
    URLEncoder.encode(this, StandardCharsets.UTF_8.name()).replace("+", "%20")
