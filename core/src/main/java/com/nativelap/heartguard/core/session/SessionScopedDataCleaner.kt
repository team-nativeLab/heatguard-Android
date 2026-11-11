package com.nativelap.heartguard.core.session

/** 세션 종료 시 사용자별 영속 데이터를 지운다. */
fun interface SessionScopedDataCleaner {
    suspend fun clearSessionData()

    suspend fun onSessionRestored(sessionIdentifier: String) = Unit
}
