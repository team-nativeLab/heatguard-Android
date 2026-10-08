package com.nativelap.heartguard.data.auth.remote

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.auth.dto.TeamLoginResponseDto

interface TeamAuthRemoteDataSource {
    suspend fun login(
        email: String,
        password: String,
    ): ApiResult<TeamLoginResponseDto>

    suspend fun logout(): ApiResult<Unit>
}
