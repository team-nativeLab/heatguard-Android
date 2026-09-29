package com.nativelap.heartguard.data.account.mapper

import com.nativelap.heartguard.domain.account.model.WithdrawReason

// 서버 reason은 자유 문자열이라 앱 화면 문구 대신 언어와 무관한 고정 코드로 보낸다.
internal fun WithdrawReason.toRequestReason(): String = when (this) {
    WithdrawReason.FIELD_WORK_ENDED -> "FIELD_WORK_ENDED"
    WithdrawReason.CHANGED_COMPANY -> "CHANGED_COMPANY"
    WithdrawReason.INCONVENIENT -> "INCONVENIENT"
    WithdrawReason.OTHER -> "OTHER"
}
