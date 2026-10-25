package com.nativelap.heartguard.core.network

/** 여러 단계로 이어지는 작업의 소유 세션을 HTTP 전송 전에 검증하는 로컬 태그다. */
data class RequestSessionGeneration(
    val generation: Long,
)
