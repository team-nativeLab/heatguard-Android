package com.nativelap.heartguard.domain.auth.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.auth.model.AppEmailCodeVerification
import com.nativelap.heartguard.domain.auth.repository.AppAuthRepository
import javax.inject.Inject

class VerifyAppEmailCodeUseCase
    @Inject
    constructor(
        private val appAuthRepository: AppAuthRepository,
    ) {
        suspend operator fun invoke(
            email: String,
            verificationCode: String,
        ): ApiResult<AppEmailCodeVerification> =
            appAuthRepository.verifyEmailCode(
                email = email,
                verificationCode = verificationCode,
            )
    }
