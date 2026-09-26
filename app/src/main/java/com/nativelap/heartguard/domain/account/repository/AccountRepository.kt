package com.nativelap.heartguard.domain.account.repository

import com.nativelap.heartguard.domain.account.model.WithdrawAccountResult

/** 로그인한 사용자 계정 자체에 대한 작업(회원탈퇴 등)을 제공한다. */
interface AccountRepository {
    suspend fun withdraw(currentPassword: String): WithdrawAccountResult
}
