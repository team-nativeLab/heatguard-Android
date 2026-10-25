package com.nativelap.heartguard.domain.checklist.usecase

import com.nativelap.heartguard.domain.checklist.repository.TeamChecklistRepository
import javax.inject.Inject

class SetChecklistItemCheckedUseCase
    @Inject
    constructor(
        private val teamChecklistRepository: TeamChecklistRepository,
    ) {
        suspend operator fun invoke(
            itemId: String,
            checked: Boolean,
        ) = teamChecklistRepository.setItemChecked(itemId, checked)
    }
