package com.nativelap.heartguard.data.account

import com.nativelap.heartguard.data.account.repository.AccountRepositoryImpl
import com.nativelap.heartguard.data.account.remote.AccountRemoteDataSource
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.account.model.WithdrawAccountResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountRepositoryImplTest {
    @Test
    fun workerWithdrawalReturnsSuccessFromWorkerEndpoint() = runTest {
        val remoteDataSource = FakeAccountRemoteDataSource()
        val repository = AccountRepositoryImpl(remoteDataSource)

        assertEquals(
            WithdrawAccountResult.Success,
            repository.withdraw(currentPassword = "current-password"),
        )
        assertEquals("current-password", remoteDataSource.receivedPassword)
    }

    private class FakeAccountRemoteDataSource : AccountRemoteDataSource {
        var receivedPassword: String? = null
            private set

        override suspend fun withdraw(currentPassword: String): ApiResult<Unit> {
            receivedPassword = currentPassword
            return ApiResult.Success(Unit)
        }
    }
}
