package com.nativelap.heartguard.data.account.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WithdrawAccountRequestDto(
    @SerialName("currentPassword")
    val currentPassword: String,
) {
    // 비밀번호 원문이 자동 생성된 toString()을 통해 로그에 노출되지 않게 한다.
    override fun toString(): String = "WithdrawAccountRequestDto(currentPassword=***)"
}
