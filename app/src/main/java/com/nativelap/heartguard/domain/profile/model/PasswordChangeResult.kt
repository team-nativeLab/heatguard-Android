package com.nativelap.heartguard.domain.profile.model

/** 비밀번호 변경 요청의 결과다. 화면은 서버 오류 코드 대신 이 결과로만 분기한다. */
sealed interface PasswordChangeResult {
    data object Success : PasswordChangeResult

    // 현재 비밀번호가 일치하지 않는다(401 INVALID_CREDENTIALS). 세션은 유지된다.
    data object InvalidCurrentPassword : PasswordChangeResult

    // 서버 비밀번호 규칙을 통과하지 못했다(400 VALIDATION_ERROR).
    data object InvalidNewPassword : PasswordChangeResult

    // 네트워크·서버 오류 등 그 밖의 이유로 실패했다.
    data object Failure : PasswordChangeResult
}
