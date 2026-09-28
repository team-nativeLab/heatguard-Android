package com.nativelap.heartguard.domain.auth.usecase

import com.nativelap.heartguard.domain.auth.repository.TeamAuthRepository
import javax.inject.Inject

class TeamLogoutUseCase @Inject constructor(
    private val teamAuthRepository: TeamAuthRepository,
) {
    suspend operator fun invoke() {
        teamAuthRepository.logout()
    }
}
