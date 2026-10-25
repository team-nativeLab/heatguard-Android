package com.nativelap.heartguard.domain.auth.model

/** 작업자 로그인의 도메인 결과다. */
sealed interface TeamLoginResult {
    data object Success : TeamLoginResult

    data object InvalidCredentials : TeamLoginResult

    data object AccountDisabled : TeamLoginResult

    data object RateLimited : TeamLoginResult

    data object Failure : TeamLoginResult
}
