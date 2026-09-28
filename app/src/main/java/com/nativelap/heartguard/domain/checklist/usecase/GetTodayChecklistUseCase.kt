package com.nativelap.heartguard.domain.checklist.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.checklist.model.Checklist
import com.nativelap.heartguard.domain.checklist.repository.ChecklistRepository
import javax.inject.Inject

class GetTodayChecklistUseCase @Inject constructor(
    private val checklistRepository: ChecklistRepository,
) {
    suspend operator fun invoke(): ApiResult<Checklist> = checklistRepository.getTodayChecklist()
}
