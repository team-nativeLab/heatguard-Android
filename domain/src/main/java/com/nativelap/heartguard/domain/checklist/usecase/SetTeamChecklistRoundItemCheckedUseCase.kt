package com.nativelap.heartguard.domain.checklist.usecase

import com.nativelap.heartguard.domain.checklist.repository.TeamChecklistRepository
import javax.inject.Inject

class SetTeamChecklistRoundItemCheckedUseCase
    @Inject
    constructor(
        private val teamChecklistRepository: TeamChecklistRepository,
    ) {
        suspend operator fun invoke(
            roundId: String,
            itemId: String,
            checked: Boolean,
        ) = teamChecklistRepository.setRoundItemChecked(roundId, itemId, checked)
    }
