package com.nativelap.heartguard.core.session

/** 동일한 토큰으로 다시 로그인해도 요청의 소유 세션을 구분한다. */
data class SessionSnapshot(
    val generation: Long,
    val accessToken: String?,
    val role: SessionRole? = null,
) {
    override fun toString(): String =
        "SessionSnapshot(generation=$generation, role=$role, hasAccessToken=${!accessToken.isNullOrBlank()})"
}
