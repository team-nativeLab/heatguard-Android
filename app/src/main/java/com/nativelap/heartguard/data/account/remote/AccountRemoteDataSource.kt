package com.nativelap.heartguard.data.account.remote

import com.nativelap.heartguard.core.network.ApiResult

/** 계정 관련 원격 요청을 담당한다. */
interface AccountRemoteDataSource {
    suspend fun withdraw(currentPassword: String): ApiResult<Unit>
}
