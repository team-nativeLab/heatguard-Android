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
        ApiResult.Failure(ApiError.Http(httpException.code()))
    } catch (_: IOException) {
        ApiResult.Failure(ApiError.Network)
    } catch (_: SerializationException) {
        ApiResult.Failure(ApiError.Serialization)
    } catch (_: Exception) {
        ApiResult.Failure(ApiError.Unknown)
    }
}
