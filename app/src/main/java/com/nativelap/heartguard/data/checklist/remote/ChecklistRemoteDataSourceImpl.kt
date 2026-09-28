package com.nativelap.heartguard.data.checklist.remote

import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.checklist.dto.ChecklistItemUpdateRequestDto
import com.nativelap.heartguard.data.checklist.dto.ChecklistItemUpdateResponseDto
import com.nativelap.heartguard.data.checklist.dto.ChecklistResponseDto
import javax.inject.Inject

class ChecklistRemoteDataSourceImpl @Inject constructor(
    private val checklistApiService: ChecklistApiService,
    private val apiExecutor: ApiExecutor,
) : ChecklistRemoteDataSource {
    override suspend fun getTodayChecklist(): ApiResult<ChecklistResponseDto> = apiExecutor.execute {
        checklistApiService.getTodayChecklist().data
            ?: error("오늘 체크리스트 응답에 data가 없습니다.")
    }

    override suspend fun setItemChecked(
        itemId: String,
        checked: Boolean,
    ): ApiResult<ChecklistItemUpdateResponseDto> = apiExecutor.execute {
        checklistApiService.setItemChecked(
            itemId = itemId,
            request = ChecklistItemUpdateRequestDto(checked = checked),
        ).data ?: error("체크리스트 완료 저장 응답에 data가 없습니다.")
    }
}
