package com.nativelap.heartguard.data.auth.repository

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.data.auth.remote.TeamAuthRemoteDataSource
import com.nativelap.heartguard.domain.auth.model.TeamLoginResult
import com.nativelap.heartguard.domain.auth.repository.TeamAuthRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class TeamAuthRepositoryImpl @Inject constructor(
    private val teamAuthRemoteDataSource: TeamAuthRemoteDataSource,
    private val sessionManager: SessionManager,
) : TeamAuthRepository {
    override suspend fun login(email: String, password: String): TeamLoginResult {
        return when (val result = teamAuthRemoteDataSource.login(email = email, password = password)) {
            is ApiResult.Success -> {
                val accessToken = result.value.accessToken
                if (accessToken.isBlank()) {
                    TeamLoginResult.Failure
                } else {
                    try {
                        sessionManager.onLoginSucceeded(SessionToken(accessToken))
                        TeamLoginResult.Success
                    } catch (cancellationException: CancellationException) {
                        throw cancellationException
                    } catch (_: Exception) {
                        TeamLoginResult.Failure
                    }
                }
            }

            is ApiResult.Failure -> result.toLoginResult()
        }
    }

    override suspend fun logout() {
        // 서버 로그아웃은 best effort다. 네트워크가 끊겨도 ViewModel이 로컬 세션을 종료한다.
        teamAuthRemoteDataSource.logout()
    }

    private fun ApiResult.Failure.toLoginResult(): TeamLoginResult = when (val apiError = error) {
        is ApiError.Http -> when (apiError.statusCode) {
            HTTP_UNAUTHORIZED -> TeamLoginResult.InvalidCredentials
            HTTP_FORBIDDEN -> TeamLoginResult.AccountDisabled
            HTTP_TOO_MANY_REQUESTS -> TeamLoginResult.RateLimited
            else -> TeamLoginResult.Failure
        }

        ApiError.Network,
        ApiError.SessionChanged,
        ApiError.Serialization,
        ApiError.Unknown,
        ApiError.LocalStorage,
        is ApiError.ServerRejected,
        -> TeamLoginResult.Failure
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val HTTP_TOO_MANY_REQUESTS = 429
    }
}
