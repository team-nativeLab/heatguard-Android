package com.nativelap.heartguard.data.account.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** DELETE /api/v1/site/profile(현장관리자 회원탈퇴) 요청 본문이다.
 * 예: {"currentPassword":"string"} */
@Serializable
data class WithdrawAccountRequestDto(
    @SerialName("currentPassword")
    val currentPassword: String,
) {
    // 로그·예외 메시지에 비밀번호 원문이 찍히지 않도록 자동 생성 toString을 대신한다.
    override fun toString(): String {
        return "WithdrawAccountRequestDto(currentPassword=***)"
    }
}
