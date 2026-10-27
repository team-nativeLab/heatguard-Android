package com.nativelap.heartguard.data.auth.mapper

import com.nativelap.heartguard.data.auth.dto.AppAccountRegistrationDto
import com.nativelap.heartguard.data.auth.dto.AppEmailCodeResponseDto
import com.nativelap.heartguard.data.auth.dto.AppEmailCodeVerificationDto
import com.nativelap.heartguard.domain.auth.model.AppAccountRegistration
import com.nativelap.heartguard.domain.auth.model.AppEmailCode
import com.nativelap.heartguard.domain.auth.model.AppEmailCodeVerification

fun AppEmailCodeResponseDto.toDomain(): AppEmailCode = AppEmailCode(expiresAt = expiresAt)

fun AppEmailCodeVerificationDto.toDomain(): AppEmailCodeVerification =
    AppEmailCodeVerification(
        verificationToken = verificationToken,
        expiresAt = expiresAt,
    )

fun AppAccountRegistrationDto.toDomain(): AppAccountRegistration =
    AppAccountRegistration(
        userId = userId,
        email = email,
        createdAt = createdAt,
    )
