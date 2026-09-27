package com.nativelap.heartguard.data.account.remote

import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.PasswordConfirmationRequest
import com.nativelap.heartguard.data.account.dto.WithdrawAccountRequestDto
import javax.inject.Inject

class AccountRemoteDataSourceImpl @Inject constructor(
    private val accountApiService: AccountApiService,
    private val apiExecutor: ApiExecutor,
) : AccountRemoteDataSource {

    override suspend fun withdraw(currentPassword: String): ApiResult<Unit> {
        return apiExecutor.execute {
            accountApiService.withdraw(
                request = WithdrawAccountRequestDto(currentPassword = currentPassword),
                passwordConfirmation = PasswordConfirmationRequest,
            )
        }
    }
}
