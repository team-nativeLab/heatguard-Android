package com.nativelap.heartguard.domain.auth.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.auth.model.AppEmailCode
import com.nativelap.heartguard.domain.auth.repository.AppAuthRepository
import javax.inject.Inject

class RequestAppEmailCodeUseCase
    @Inject
    constructor(
        private val appAuthRepository: AppAuthRepository,
    ) {
        suspend operator fun invoke(email: String): ApiResult<AppEmailCode> = appAuthRepository.requestEmailCode(email)
    }
