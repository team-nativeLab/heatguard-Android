package com.nativelap.heartguard.core.network

/** 원격 호출의 성공 또는 공통 오류를 표현한다. */
sealed interface ApiResult<out Value> {
    data class Success<Value>(val value: Value) : ApiResult<Value>

    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

/** HTTP 응답, 연결, 변환 과정에서 발생한 앱 내부 공통 오류다. */
sealed interface ApiError {
    // errorCode는 서버 공통 오류 본문의 error.code다(예: INVALID_CREDENTIALS). 본문이 없거나 JSON이 아니면 null이다.
    data class Http(
        val statusCode: Int,
        val errorCode: String? = null,
    ) : ApiError

    data object Network : ApiError

    data object Serialization : ApiError

    data object Unknown : ApiError
}

/** RepositoryImpl이 RemoteDataSource의 DTO 결과를 도메인 모델로 바꿀 때 쓰는 공통 변환이다.
 * Failure는 그대로 통과시키고 Success 값만 [transform]한다 — 여러 RepositoryImpl에 흩어져 있던
 * `when (result) { is Success -> Success(mapper); is Failure -> result }` 반복을 대신한다. */
inline fun <Value, MappedValue> ApiResult<Value>.map(
    transform: (Value) -> MappedValue,
): ApiResult<MappedValue> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(value))
    is ApiResult.Failure -> this
}
