package com.nativelap.heartguard.data.checklist.remote

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.checklist.dto.ChecklistItemStatusDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistResponseDto
import com.nativelap.heartguard.data.checklist.dto.UpdateChecklistItemRequestDto

interface TeamChecklistRemoteDataSource {
    suspend fun getChecklist(date: String): ApiResult<TeamChecklistResponseDto>

    suspend fun setItemChecked(
        itemId: String,
        request: UpdateChecklistItemRequestDto,
    ): ApiResult<ChecklistItemStatusDto>
}
