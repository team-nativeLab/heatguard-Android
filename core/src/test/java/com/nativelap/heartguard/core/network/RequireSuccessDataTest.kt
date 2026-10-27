package com.nativelap.heartguard.core.network

import kotlin.test.Test
import kotlin.test.assertEquals

class RequireSuccessDataTest {
    @Test
    fun `requireSuccessData returns valid response`() {
        val result = ApiResult.Success(ApiEnvelope(success = true, data = "worker"))

        assertEquals(ApiResult.Success("worker"), result.requireSuccessData { it.isNotBlank() })
    }

    @Test
    fun `requireSuccessData maps explicit server rejection`() {
        val result =
            ApiResult.Success(
                ApiEnvelope<String>(
                    success = false,
                    error = ApiEnvelopeError(code = "INVALID_CREDENTIALS", message = "인증 실패"),
                ),
            )

        assertEquals(
            ApiResult.Failure(ApiError.ServerRejected("INVALID_CREDENTIALS", "인증 실패")),
            result.requireSuccessData(),
        )
    }

    @Test
    fun `requireSuccessData rejects missing data contradictory envelope and invalid data`() {
        val missingData = ApiResult.Success(ApiEnvelope<String>(success = true))
        val contradictoryEnvelope =
            ApiResult.Success(
                ApiEnvelope(success = true, data = "worker", error = ApiEnvelopeError(code = "ERROR")),
            )
        val invalidData = ApiResult.Success(ApiEnvelope(success = true, data = ""))

        assertEquals(ApiResult.Failure(ApiError.Serialization), missingData.requireSuccessData())
        assertEquals(ApiResult.Failure(ApiError.Serialization), contradictoryEnvelope.requireSuccessData())
        assertEquals(ApiResult.Failure(ApiError.Serialization), invalidData.requireSuccessData { it.isNotBlank() })
    }

    @Test
    fun `requireSuccessData preserves existing failure`() {
        val result: ApiResult<ApiEnvelope<String>> = ApiResult.Failure(ApiError.Network)

        assertEquals(ApiResult.Failure(ApiError.Network), result.requireSuccessData())
    }
}
