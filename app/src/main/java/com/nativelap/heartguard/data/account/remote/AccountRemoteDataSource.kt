package com.nativelap.heartguard.data.account.remote

import com.nativelap.heartguard.core.network.ApiResult

interface AccountRemoteDataSource {
    suspend fun withdraw(
        currentPassword: String,
        reason: String?,
    ): ApiResult<Unit>
}
