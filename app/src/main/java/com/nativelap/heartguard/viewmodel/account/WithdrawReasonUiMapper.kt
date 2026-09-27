package com.nativelap.heartguard.viewmodel.account

import androidx.annotation.StringRes
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.account.model.WithdrawReason

/** 탈퇴 사유를 화면에 표시할 문구 리소스로 바꾼다. Domain enum에 화면 문구를 넣지 않기 위해 UI 계층에 둔다. */
@StringRes
fun WithdrawReason.labelResId(): Int {
    return when (this) {
        WithdrawReason.FIELD_WORK_ENDED -> R.string.withdraw_reason_field_work_ended
        WithdrawReason.CHANGED_COMPANY -> R.string.withdraw_reason_changed_company
        WithdrawReason.INCONVENIENT -> R.string.withdraw_reason_inconvenient
        WithdrawReason.OTHER -> R.string.withdraw_reason_other
    }
}
