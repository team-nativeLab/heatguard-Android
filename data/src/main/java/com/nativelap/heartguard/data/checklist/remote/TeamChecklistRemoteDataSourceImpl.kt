package com.nativelap.heartguard.data.checklist.remote

import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.requireSuccessData
import com.nativelap.heartguard.data.checklist.dto.ChecklistItemStatusDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistResponseDto
import com.nativelap.heartguard.data.checklist.dto.UpdateChecklistItemRequestDto
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
    }
