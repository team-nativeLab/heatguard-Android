package com.nativelap.heartguard.data.account.remote

import com.nativelap.heartguard.core.network.PasswordConfirmationRequest
import com.nativelap.heartguard.data.account.dto.WithdrawAccountRequestDto
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.Tag

interface AccountApiService {
    // DELETE /api/v1/team/profile — 현재 비밀번호 확인 후 팀 계정을 비활성화한다. 성공 시 본문이 없다.
    // DELETE에 본문을 실어야 해서 @DELETE 대신 @HTTP(hasBody = true)를 쓴다.
    @HTTP(
        method = "DELETE",
        path = "api/v1/team/profile",
        hasBody = true,
    )
    suspend fun withdraw(
        @Body request: WithdrawAccountRequestDto,
        @Tag passwordConfirmation: PasswordConfirmationRequest,
    )
}
