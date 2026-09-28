package com.nativelap.heartguard.data.checklist.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.map
import com.nativelap.heartguard.data.checklist.mapper.toDomain
import com.nativelap.heartguard.data.checklist.remote.ChecklistRemoteDataSource
import com.nativelap.heartguard.domain.checklist.model.Checklist
import com.nativelap.heartguard.domain.checklist.model.ChecklistItemUpdate
import com.nativelap.heartguard.domain.checklist.repository.ChecklistRepository
import javax.inject.Inject

class ChecklistRepositoryImpl @Inject constructor(
    private val checklistRemoteDataSource: ChecklistRemoteDataSource,
) : ChecklistRepository {
    override suspend fun getTodayChecklist(): ApiResult<Checklist> =
        checklistRemoteDataSource.getTodayChecklist().map { it.toDomain() }

    override suspend fun setItemChecked(
        itemId: String,
        checked: Boolean,
    ): ApiResult<ChecklistItemUpdate> =
        checklistRemoteDataSource.setItemChecked(itemId, checked).map { it.toDomain() }
}
