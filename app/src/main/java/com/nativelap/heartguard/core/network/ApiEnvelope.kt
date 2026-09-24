package com.nativelap.heartguard.core.network

import kotlinx.serialization.Serializable

/** API 명세서의 모든 응답이 공통으로 쓰는 최상위 래퍼다.
 * 예: {"success":true,"data":{...},"error":null,"meta":{"requestId":"req_01"}}
 * 각 ApiService는 이 타입으로 감싼 DTO를 반환하고, RemoteDataSourceImpl이 [data]만 꺼내 쓴다. */
@Serializable
data class ApiEnvelope<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiEnvelopeError? = null,
    val meta: ApiEnvelopeMeta? = null,
)

@Serializable
data class ApiEnvelopeError(
    val code: String? = null,
    val message: String? = null,
)

@Serializable
data class ApiEnvelopeMeta(
    val requestId: String? = null,
)
