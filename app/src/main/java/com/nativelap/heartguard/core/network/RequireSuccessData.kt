package com.nativelap.heartguard.core.network

/** 성공 봉투와 필수 데이터를 검증하고 모순된 응답을 성공으로 노출하지 않는다. */
fun <Response> ApiResult<ApiEnvelope<Response>>.requireSuccessData(
    isValid: (Response) -> Boolean = { true },
): ApiResult<Response> =
    when (this) {
        is ApiResult.Failure -> {
            this
        }

        is ApiResult.Success -> {
            val envelope = value
            val response = envelope.data
            when {
                !envelope.success && response == null && !envelope.error?.code.isNullOrBlank() -> {
                    ApiResult.Failure(ApiError.ServerRejected(requireNotNull(envelope.error?.code)))
                }

                envelope.success && envelope.error == null && response != null && isValid(response) -> {
                    ApiResult.Success(response)
                }

                else -> {
                    ApiResult.Failure(ApiError.Serialization)
                }
            }
        }
    }
