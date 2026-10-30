package com.nativelap.heartguard.domain.checklist.usecase

import com.nativelap.heartguard.domain.checklist.repository.TeamChecklistRepository
import java.time.LocalDate
import javax.inject.Inject

class GetTeamChecklistRoundsUseCase
    @Inject
    constructor(
        private val teamChecklistRepository: TeamChecklistRepository,
    ) {
        suspend operator fun invoke(date: LocalDate) = teamChecklistRepository.getRounds(date)
    }
