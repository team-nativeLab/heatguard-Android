package com.nativelap.heartguard.data.account

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.account.dto.WithdrawAccountRequestDto
import com.nativelap.heartguard.data.account.remote.AccountRemoteDataSource
import com.nativelap.heartguard.data.account.repository.AccountRepositoryImpl
import com.nativelap.heartguard.domain.account.model.WithdrawAccountResult
import com.nativelap.heartguard.domain.account.model.WithdrawReason
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
            repository.withdraw(
                currentPassword = "current-password",
                reason = null,
            ),
        )
        assertEquals("current-password", remoteDataSource.receivedPassword)
        assertEquals(null, remoteDataSource.receivedReason)
    }

    @Test
    fun workerWithdrawalSendsSelectedReasonAsCode() = runTest {
        val remoteDataSource = FakeAccountRemoteDataSource()
        val repository = AccountRepositoryImpl(remoteDataSource)

        repository.withdraw(
            currentPassword = "current-password",
            reason = WithdrawReason.CHANGED_COMPANY,
        )

        assertEquals("CHANGED_COMPANY", remoteDataSource.receivedReason)
    }

    @Test
    fun withdrawRequestToStringMasksPassword() {
        val request = WithdrawAccountRequestDto(
            currentPassword = "current-password",
            reason = "OTHER",
        )

        assertEquals(false, request.toString().contains("current-password"))
    }

    private class FakeAccountRemoteDataSource : AccountRemoteDataSource {
        var receivedPassword: String? = null
            private set
        var receivedReason: String? = null
            private set

        override suspend fun withdraw(
            currentPassword: String,
            reason: String?,
        ): ApiResult<Unit> {
            receivedPassword = currentPassword
            receivedReason = reason
            return ApiResult.Success(Unit)
        }
    }
}
