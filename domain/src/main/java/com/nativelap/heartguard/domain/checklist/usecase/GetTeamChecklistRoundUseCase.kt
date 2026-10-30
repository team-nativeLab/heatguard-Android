package com.nativelap.heartguard.domain.checklist.usecase

import com.nativelap.heartguard.domain.checklist.repository.TeamChecklistRepository
import java.time.LocalDate
import javax.inject.Inject

class GetTeamChecklistRoundUseCase
    @Inject
    constructor(
        private val teamChecklistRepository: TeamChecklistRepository,
    ) {
        suspend operator fun invoke(
            roundId: String,
            date: LocalDate,
        ) = teamChecklistRepository.getRound(roundId, date)
    }
