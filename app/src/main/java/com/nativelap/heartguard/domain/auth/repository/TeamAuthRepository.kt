package com.nativelap.heartguard.domain.auth.repository

import com.nativelap.heartguard.domain.auth.model.TeamLoginResult

interface TeamAuthRepository {
    suspend fun login(
        email: String,
        password: String,
    ): TeamLoginResult

    suspend fun logout()
}
