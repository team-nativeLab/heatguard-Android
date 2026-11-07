package com.nativelap.heartguard.core.network

import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionRole
import com.nativelap.heartguard.core.session.SessionSnapshot
import com.nativelap.heartguard.core.session.SelectedTeamRequestSnapshot
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/** 체크리스트 API에만 세션에서 확인한 팀 ID를 전달한다. */
class SelectedTeamIdInterceptor(
    private val sessionManager: SessionManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestSnapshot = request.tag(SessionSnapshot::class.java) ?: throw SessionChangedException()
        if (requestSnapshot.role != SessionRole.TEAM_MEMBER || requestSnapshot != sessionManager.getSnapshot()) {
            throw SessionChangedException()
        }
        val selectedTeamSnapshot = request.tag(SelectedTeamRequestSnapshot::class.java)
            ?: throw TeamNotSelectedException()
        if (!sessionManager.isSelectedTeamRequestSnapshotCurrent(requestSnapshot, selectedTeamSnapshot)) {
            throw SessionChangedException()
        }
        return chain.proceed(request.newBuilder().header(TEAM_ID_HEADER, selectedTeamSnapshot.teamId).build())
    }

    private companion object {
        const val TEAM_ID_HEADER = "X-Team-Id"
    }
}

class TeamNotSelectedException : IOException("A team must be selected before this request")
