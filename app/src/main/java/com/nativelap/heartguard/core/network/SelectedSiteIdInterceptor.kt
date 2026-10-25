package com.nativelap.heartguard.core.network

import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionSnapshot
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/** 선택 현장 범위 API에만 현재 세션의 선택 현장 ID를 전달한다. */
class SelectedSiteIdInterceptor(
    private val sessionManager: SessionManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestSnapshot = request.tag(SessionSnapshot::class.java)
        val currentSnapshot = sessionManager.getSnapshot()
        if (requestSnapshot != currentSnapshot) {
            throw SessionChangedException()
        }

        val selectedSiteId =
            request.tag(SelectedSiteIdSnapshot::class.java)?.siteId
                ?: throw SiteNotSelectedException()
        val selectedRequest =
            request
                .newBuilder()
                .header(SITE_ID_HEADER, selectedSiteId)
                .build()
        return chain.proceed(selectedRequest)
    }

    private companion object {
        const val SITE_ID_HEADER = "X-Site-Id"
    }
}

class SiteNotSelectedException : IOException("An active site must be selected before this request")
