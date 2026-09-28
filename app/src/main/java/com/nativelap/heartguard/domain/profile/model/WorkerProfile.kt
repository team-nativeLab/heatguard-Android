package com.nativelap.heartguard.domain.profile.model

/** 로그인한 작업자 계정 정보다. 서버가 비워 둔 값은 null이다. */
data class WorkerProfile(
    val userId: String,
    val name: String?,
    val email: String?,
)
