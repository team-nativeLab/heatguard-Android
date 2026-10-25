package com.nativelap.heartguard.data.auth.remote

import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.auth.dto.TeamLoginRequestDto
import com.nativelap.heartguard.data.auth.dto.TeamLoginResponseDto
import javax.inject.Inject

class TeamAuthRemoteDataSourceImpl
    @Inject
    constructor(
        private val teamLoginApiService: TeamLoginApiService,
        private val teamSessionApiService: TeamSessionApiService,
        private val apiExecutor: ApiExecutor,
    ) : TeamAuthRemoteDataSource {
        override suspend fun login(
            email: String,
            password: String,
        ): ApiResult<TeamLoginResponseDto> =
            apiExecutor.execute {
                val envelope =
                    teamLoginApiService.login(
                        TeamLoginRequestDto(email = email, password = password),
                    )
                if (!envelope.success) {
                    error("작업자 로그인 응답이 실패 상태입니다.")
                }
                envelope.data ?: error("작업자 로그인 응답에 data가 없습니다.")
            }

        override suspend fun logout(): ApiResult<Unit> =
            apiExecutor.execute {
                teamSessionApiService.logout()
            }
    }
