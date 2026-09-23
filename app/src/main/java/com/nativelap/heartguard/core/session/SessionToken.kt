package com.nativelap.heartguard.core.session

/** 팀 로그인 응답에서 확인된 access token이다. */
data class SessionToken(
    val accessToken: String,
)
