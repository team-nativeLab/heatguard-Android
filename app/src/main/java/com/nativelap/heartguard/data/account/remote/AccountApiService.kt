package com.nativelap.heartguard.data.account.remote

import com.nativelap.heartguard.core.network.PasswordConfirmationRequest
import com.nativelap.heartguard.data.account.dto.WithdrawAccountRequestDto
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.Tag

interface AccountApiService {
    @HTTP(method = "DELETE", path = "api/v1/team/profile", hasBody = true)
    suspend fun withdraw(
        @Body request: WithdrawAccountRequestDto,
        @Tag passwordConfirmationRequest: PasswordConfirmationRequest,
    )
}
