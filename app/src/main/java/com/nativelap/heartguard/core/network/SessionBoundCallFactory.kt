package com.nativelap.heartguard.core.network

import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionSnapshot
import java.io.IOException
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request

/** 인증 API 요청이 만들어진 시점의 세션을 태그로 붙여, 전송 시점에 세션이 바뀌었는지 판단하게 한다.
 * Retrofit은 suspend 호출을 시작한 코루틴에서 동기로 [newCall]을 부르므로 이 시점이 요청 생성 시점이다. */
class SessionBoundCallFactory(
    private val delegateClient: OkHttpClient,
    private val sessionManager: SessionManager,
) : Call.Factory {
    override fun newCall(request: Request): Call {
        val owningSnapshot = sessionManager.getSnapshot()
        val expectedGeneration = request.tag(RequestSessionGeneration::class.java)?.generation
        if (expectedGeneration != null && expectedGeneration != owningSnapshot.generation) {
            throw SessionChangedException()
        }
        val sessionBoundRequest = request.newBuilder()
            .tag(SessionSnapshot::class.java, owningSnapshot)
            .build()
        return delegateClient.newCall(sessionBoundRequest)
    }
}

/** 요청을 만든 세션이 전송 전에 끝나거나 다른 세션으로 바뀌어 전송을 중단했음을 알린다.
 * OkHttp 비동기 호출에서 크래시 없이 실패로 전달되도록 IOException을 상속한다. */
class SessionChangedException : IOException("Session changed before the request was sent")
