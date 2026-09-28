package com.nativelap.heartguard.data.checklist.remote

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.checklist.dto.ChecklistItemUpdateResponseDto
import com.nativelap.heartguard.data.checklist.dto.ChecklistResponseDto

interface ChecklistRemoteDataSource {
    suspend fun getTodayChecklist(): ApiResult<ChecklistResponseDto>

    suspend fun setItemChecked(
        itemId: String,
        checked: Boolean,
    ): ApiResult<ChecklistItemUpdateResponseDto>
}
