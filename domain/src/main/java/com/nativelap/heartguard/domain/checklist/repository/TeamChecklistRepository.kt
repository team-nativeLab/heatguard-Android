package com.nativelap.heartguard.domain.checklist.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.checklist.model.ChecklistItemCheckUpdate
import com.nativelap.heartguard.domain.checklist.model.TeamChecklist

interface TeamChecklistRepository {
    suspend fun getChecklist(): ApiResult<TeamChecklist>

    suspend fun setItemChecked(
        itemId: String,
        checked: Boolean,
    ): ApiResult<ChecklistItemCheckUpdate>
}
