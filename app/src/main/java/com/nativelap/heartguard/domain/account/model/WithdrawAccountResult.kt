package com.nativelap.heartguard.domain.account.model

/** 회원탈퇴 요청의 결과다. 화면은 서버 오류 코드 대신 이 결과로만 분기한다. */
sealed interface WithdrawAccountResult {
    data object Success : WithdrawAccountResult

    // 현재 비밀번호가 일치하지 않는다. 사용자가 비밀번호를 다시 입력하면 재시도할 수 있다.
    data object InvalidPassword : WithdrawAccountResult

    // 네트워크·서버 오류 등 비밀번호 외의 이유로 실패했다.
    data object Failure : WithdrawAccountResult
}
