package com.nativelap.heartguard.data.emergency.remote

import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.TeamTokenProvider
import com.nativelap.heartguard.data.emergency.dto.EmergencyCallRequestDto
import com.nativelap.heartguard.data.emergency.dto.EmergencyCallResponseDto
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class EmergencyCallRemoteDataSourceImpl @Inject constructor(
    private val emergencyCallApiService: EmergencyCallApiService,
    private val teamTokenProvider: TeamTokenProvider,
    private val apiExecutor: ApiExecutor,
) : EmergencyCallRemoteDataSource {

    override suspend fun registerEmergencyCall(message: String?): ApiResult<EmergencyCallResponseDto> =
        apiExecutor.execute {
            val envelope = emergencyCallApiService.registerEmergencyCall(
                teamToken = teamTokenProvider.currentTeamToken(),
                // Idempotency-Key로 동일 등록 요청이 중복 생성되지 않게 한다(API 명세서 '기타' 항목).
                idempotencyKey = UUID.randomUUID().toString(),
                request = EmergencyCallRequestDto(
                    message = message,
                    clientOccurredAt = Instant.now().toString(),
                ),
            )
            envelope.data ?: error("긴급호출 등록 응답에 data가 없습니다.")
        }

    override suspend fun getCurrentEmergencyCall(): ApiResult<EmergencyCallResponseDto> = apiExecutor.execute {
        val envelope = emergencyCallApiService.getCurrentEmergencyCall(teamTokenProvider.currentTeamToken())
        envelope.data ?: error("긴급호출 상태 응답에 data가 없습니다.")
    }
}
