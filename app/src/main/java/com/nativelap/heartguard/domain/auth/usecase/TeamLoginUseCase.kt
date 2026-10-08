package com.nativelap.heartguard.domain.auth.usecase

import com.nativelap.heartguard.domain.auth.model.TeamLoginResult
import com.nativelap.heartguard.domain.auth.repository.TeamAuthRepository
import javax.inject.Inject

class TeamLoginUseCase
    @Inject
    constructor(
        private val teamAuthRepository: TeamAuthRepository,
    ) {
        suspend operator fun invoke(
            email: String,
            password: String,
        ): TeamLoginResult = teamAuthRepository.login(email = email, password = password)
    }
