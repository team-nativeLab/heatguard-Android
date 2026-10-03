package com.nativelap.heartguard.data.account.repository

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.account.mapper.toRequestReason
import com.nativelap.heartguard.data.account.remote.AccountRemoteDataSource
import com.nativelap.heartguard.domain.account.model.WithdrawAccountResult
import com.nativelap.heartguard.domain.account.model.WithdrawReason
import com.nativelap.heartguard.domain.account.repository.AccountRepository
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val accountRemoteDataSource: AccountRemoteDataSource,
) : AccountRepository {

    override suspend fun withdraw(
        currentPassword: String,
        reason: WithdrawReason?,
    ): WithdrawAccountResult {
        val withdrawResult = accountRemoteDataSource.withdraw(
            currentPassword = currentPassword,
            reason = reason?.toRequestReason(),
        )
        return when (val result = withdrawResult) {
            is ApiResult.Success -> WithdrawAccountResult.Success
            is ApiResult.Failure -> result.toWithdrawResult()
        }
    }

    private fun ApiResult.Failure.toWithdrawResult(): WithdrawAccountResult = when (val apiError = error) {
        is ApiError.Http -> if (
            apiError.statusCode == HTTP_UNAUTHORIZED && apiError.errorCode == INVALID_CREDENTIALS_CODE
        ) {
            WithdrawAccountResult.InvalidPassword
        } else {
            WithdrawAccountResult.Failure
        }

        ApiError.Network,
        ApiError.SessionChanged,
        ApiError.Serialization,
        ApiError.Unknown,
        ApiError.LocalStorage,
        is ApiError.ServerRejected,
        -> WithdrawAccountResult.Failure
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        const val INVALID_CREDENTIALS_CODE = "INVALID_CREDENTIALS"
    }
}
