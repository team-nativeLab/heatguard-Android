package com.nativelap.heartguard.core.session

/** 백엔드 세션 토큰을 읽고 저장하는 로컬 보안 저장소 계약이다. */
interface TokenStorage {
    fun readAccessToken(): String?

    fun saveAccessToken(accessToken: String)

    fun readSessionRole(): SessionRole? = null

    fun saveSession(
        accessToken: String,
        role: SessionRole,
    ) {
        saveAccessToken(accessToken)
    }

    fun readSelectedSiteId(): String? = null

    fun saveSelectedSiteId(siteId: String) = Unit

    fun clearSelectedSiteId() = Unit

    fun clear()
}
