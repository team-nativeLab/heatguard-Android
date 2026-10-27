package com.nativelap.heartguard.domain.auth.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.auth.model.AppAccountRegistration
import com.nativelap.heartguard.domain.auth.repository.AppAuthRepository
import javax.inject.Inject

class RegisterAppAccountUseCase
    @Inject
    constructor(
        private val appAuthRepository: AppAuthRepository,
    ) {
        suspend operator fun invoke(
            email: String,
            password: String,
            confirmPassword: String,
            verificationToken: String,
        ): ApiResult<AppAccountRegistration> =
            appAuthRepository.register(
                email = email,
                password = password,
                confirmPassword = confirmPassword,
                verificationToken = verificationToken,
            )
    }
