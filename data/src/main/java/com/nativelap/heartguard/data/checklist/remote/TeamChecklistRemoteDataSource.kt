package com.nativelap.heartguard.data.checklist.remote

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.checklist.dto.ChecklistItemStatusDto
import com.nativelap.heartguard.data.checklist.dto.ChecklistRoundItemStatusDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistResponseDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistRoundDetailResponseDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistRoundsResponseDto
import com.nativelap.heartguard.data.checklist.dto.UpdateChecklistItemRequestDto
import com.nativelap.heartguard.data.checklist.dto.UpdateChecklistRoundItemRequestDto

interface TeamChecklistRemoteDataSource {
    suspend fun getChecklist(): ApiResult<TeamChecklistResponseDto>

    suspend fun setItemChecked(
        itemId: String,
        request: UpdateChecklistItemRequestDto,
    ): ApiResult<ChecklistItemStatusDto>

    suspend fun getRounds(date: String): ApiResult<TeamChecklistRoundsResponseDto>

    suspend fun getRound(
        roundId: String,
        date: String,
    ): ApiResult<TeamChecklistRoundDetailResponseDto>

    suspend fun setRoundItemChecked(
        roundId: String,
        itemId: String,
        request: UpdateChecklistRoundItemRequestDto,
    ): ApiResult<ChecklistRoundItemStatusDto>
}
