package com.nativelap.heartguard.core.network

import javax.inject.Inject
import retrofit2.HttpException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import java.io.IOException

/** API 호출의 예상 가능한 실패를 공통 결과로 변환하고 취소를 보존한다. */
class ApiExecutor @Inject constructor() {
    suspend fun <Value> execute(request: suspend () -> Value): ApiResult<Value> = try {
        ApiResult.Success(request())
    } catch (cancellationException: CancellationException) {
        throw cancellationException
    } catch (httpException: HttpException) {
        ApiResult.Failure(
            ApiError.Http(
                statusCode = httpException.code(),
                errorCode = httpException.readErrorCode(),
            ),
        )
    } catch (_: IOException) {
        ApiResult.Failure(ApiError.Network)
    } catch (_: SerializationException) {
        ApiResult.Failure(ApiError.Serialization)
    } catch (_: Exception) {
        ApiResult.Failure(ApiError.Unknown)
    }

    // 오류 본문은 Retrofit이 이미 메모리에 버퍼링해 두었으므로 한 번 읽어도 안전하다.
    // 읽기·파싱에 실패해도 원래 HTTP 오류 변환을 막지 않도록 모든 실패를 null로 처리한다.
    private fun HttpException.readErrorCode(): String? {
        val errorBody = response()?.errorBody() ?: return null

        if (errorBody.contentLength() > ApiErrorCodeReader.MAX_ERROR_BODY_BYTES) {
            return null
        }

        return try {
            ApiErrorCodeReader.read(errorBody.string())
        } catch (_: IOException) {
            null
        }
    }
}
