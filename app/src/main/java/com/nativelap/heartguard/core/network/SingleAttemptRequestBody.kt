package com.nativelap.heartguard.core.network

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink

/** 서버 처리 여부가 불명인 POST 본문을 OkHttp가 자동으로 다시 보내지 못하게 한다. */
internal class SingleAttemptRequestBody(
    private val delegateBody: RequestBody,
) : RequestBody() {
    override fun contentType(): MediaType? = delegateBody.contentType()

    override fun contentLength(): Long = delegateBody.contentLength()

    override fun isOneShot(): Boolean = true

    override fun writeTo(sink: BufferedSink) {
        delegateBody.writeTo(sink)
    }
}
