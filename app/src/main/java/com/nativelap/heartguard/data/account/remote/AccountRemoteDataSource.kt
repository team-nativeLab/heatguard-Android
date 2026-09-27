package com.nativelap.heartguard.data.account.remote

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.account.model.WithdrawReason

/** 계정 관련 원격 요청을 담당한다. */
interface AccountRemoteDataSource {
    suspend fun withdraw(
        password: String,
        reason: WithdrawReason?,
    ): ApiResult<Unit>
}
