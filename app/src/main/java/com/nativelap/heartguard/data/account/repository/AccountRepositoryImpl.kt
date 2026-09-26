package com.nativelap.heartguard.data.account.repository

import com.nativelap.heartguard.data.account.mapper.toWithdrawAccountResult
import com.nativelap.heartguard.data.account.remote.AccountRemoteDataSource
import com.nativelap.heartguard.domain.account.model.WithdrawAccountResult
import com.nativelap.heartguard.domain.account.repository.AccountRepository
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val accountRemoteDataSource: AccountRemoteDataSource,
) : AccountRepository {

    override suspend fun withdraw(currentPassword: String): WithdrawAccountResult {
        return accountRemoteDataSource
            .withdraw(currentPassword = currentPassword)
            .toWithdrawAccountResult()
    }
}
