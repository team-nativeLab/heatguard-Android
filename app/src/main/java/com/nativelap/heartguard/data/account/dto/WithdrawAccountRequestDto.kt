package com.nativelap.heartguard.data.account.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** DELETE /api/v1/team/profile 요청 본문이다. [reason]은 선택 값(최대 1000자)이며 null이면 보내지 않는다. */
@Serializable
data class WithdrawAccountRequestDto(
    @SerialName("currentPassword")
    val currentPassword: String,
    @SerialName("reason")
    val reason: String? = null,
) {
    // 비밀번호 원문이 자동 생성된 toString()을 통해 로그에 노출되지 않게 한다.
    override fun toString(): String = "WithdrawAccountRequestDto(currentPassword=***, reason=$reason)"
}
