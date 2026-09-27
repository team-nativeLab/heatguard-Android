package com.nativelap.heartguard.data.account.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.account.remote.AccountRemoteDataSource
import com.nativelap.heartguard.domain.account.model.WithdrawReason
import com.nativelap.heartguard.domain.account.repository.AccountRepository
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val accountRemoteDataSource: AccountRemoteDataSource,
) : AccountRepository {

    override suspend fun withdraw(
        password: String,
        reason: WithdrawReason?,
    ): ApiResult<Unit> {
        return accountRemoteDataSource.withdraw(
            password = password,
            reason = reason,
        )
    }
}
