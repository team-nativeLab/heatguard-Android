package com.nativelap.heartguard.domain.checklist.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.checklist.model.Checklist
import com.nativelap.heartguard.domain.checklist.model.ChecklistItemUpdate

interface ChecklistRepository {
    suspend fun getTodayChecklist(): ApiResult<Checklist>

    suspend fun setItemChecked(itemId: String, checked: Boolean): ApiResult<ChecklistItemUpdate>
}
