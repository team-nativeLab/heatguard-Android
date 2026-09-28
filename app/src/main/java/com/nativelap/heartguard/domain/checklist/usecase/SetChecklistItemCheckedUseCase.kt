package com.nativelap.heartguard.domain.checklist.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.checklist.model.ChecklistItemUpdate
import com.nativelap.heartguard.domain.checklist.repository.ChecklistRepository
import javax.inject.Inject

class SetChecklistItemCheckedUseCase @Inject constructor(
    private val checklistRepository: ChecklistRepository,
) {
    suspend operator fun invoke(
        itemId: String,
        checked: Boolean,
    ): ApiResult<ChecklistItemUpdate> = checklistRepository.setItemChecked(itemId, checked)
}
