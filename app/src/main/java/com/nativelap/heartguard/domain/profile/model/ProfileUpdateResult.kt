package com.nativelap.heartguard.domain.profile.model

/** 내 정보 수정 요청의 결과다. 화면은 서버 오류 코드 대신 이 결과로만 분기한다. */
sealed interface ProfileUpdateResult {
    data class Success(
        val profile: WorkerProfile,
    ) : ProfileUpdateResult

    // 조회 후 다른 곳에서 정보가 바뀌어 저장하지 못했다(409).
    data object Conflict : ProfileUpdateResult

    // 네트워크·서버 오류 등 그 밖의 이유로 실패했다.
    data object Failure : ProfileUpdateResult
}
