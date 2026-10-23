package com.nativelap.heartguard.domain.auth.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.auth.repository.AppAuthRepository
import javax.inject.Inject

class LoginAppAccountUseCase
    @Inject
    constructor(
        private val appAuthRepository: AppAuthRepository,
    ) {
        suspend operator fun invoke(
            email: String,
            password: String,
        ): ApiResult<Unit> = appAuthRepository.login(email = email, password = password)
    }
