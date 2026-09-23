package com.nativelap.heartguard.core.network

/** 원격 호출의 성공 또는 공통 오류를 표현한다. */
sealed interface ApiResult<out Value> {
    data class Success<Value>(val value: Value) : ApiResult<Value>

    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

/** HTTP 응답, 연결, 변환 과정에서 발생한 앱 내부 공통 오류다. */
sealed interface ApiError {
    data class Http(val statusCode: Int) : ApiError

    data object Network : ApiError

    data object Serialization : ApiError

    data object Unknown : ApiError
}
