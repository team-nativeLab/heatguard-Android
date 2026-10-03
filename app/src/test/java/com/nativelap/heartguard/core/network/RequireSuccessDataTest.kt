package com.nativelap.heartguard.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class RequireSuccessDataTest {
    @Test
    fun `정상 데이터만 성공이고 모순 응답은 변환 오류다`() {
        assertEquals(ApiResult.Success("id"), ApiResult.Success(ApiEnvelope(true, "id")).requireSuccessData())
        listOf(
            ApiEnvelope(false, "id", ApiEnvelopeError("VALIDATION_ERROR")),
            ApiEnvelope(true, "id", ApiEnvelopeError("INTERNAL_ERROR")),
            ApiEnvelope<String>(true),
            ApiEnvelope<String>(false),
        ).forEach { envelope ->
            assertEquals(ApiResult.Failure(ApiError.Serialization), ApiResult.Success(envelope).requireSuccessData())
        }
    }

    @Test
    fun `거절 코드를 보존하고 빈 ID 검증을 통과시키지 않는다`() {
        assertEquals(
            ApiResult.Failure(ApiError.ServerRejected("VALIDATION_ERROR")),
            ApiResult.Success(ApiEnvelope<String>(false, error = ApiEnvelopeError("VALIDATION_ERROR"))).requireSuccessData(),
        )
        assertEquals(
            ApiResult.Failure(ApiError.Serialization),
            ApiResult.Success(ApiEnvelope(true, " ")).requireSuccessData { it.isNotBlank() },
        )
    }
}
