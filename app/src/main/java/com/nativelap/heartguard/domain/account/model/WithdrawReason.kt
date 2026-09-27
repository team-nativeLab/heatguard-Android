package com.nativelap.heartguard.domain.account.model

/** 회원탈퇴 시 사용자가 고를 수 있는 탈퇴 사유다. Figma 17_회원탈퇴_안내의 선택지와 일대일로 대응한다. */
enum class WithdrawReason {
    FIELD_WORK_ENDED,
    CHANGED_COMPANY,
    INCONVENIENT,
    OTHER,
}
