package com.nativelap.heartguard.domain.auth.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.auth.repository.AppAuthRepository
import javax.inject.Inject

class LogoutAppAccountUseCase
    @Inject
    constructor(
        private val appAuthRepository: AppAuthRepository,
    ) {
        suspend operator fun invoke(): ApiResult<Unit> = appAuthRepository.logout()
    }
