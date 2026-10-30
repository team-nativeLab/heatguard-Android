package com.nativelap.heartguard.data.checklist.repository

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.checklist.dto.ChecklistItemStatusDto
import com.nativelap.heartguard.data.checklist.dto.ChecklistRoundItemStatusDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistResponseDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistRoundDetailResponseDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistRoundsResponseDto
import com.nativelap.heartguard.data.checklist.dto.UpdateChecklistItemRequestDto
import com.nativelap.heartguard.data.checklist.dto.UpdateChecklistRoundItemRequestDto
import com.nativelap.heartguard.data.checklist.remote.TeamChecklistRemoteDataSource
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class TeamChecklistRepositoryImplTest {
    @Test
    fun `getChecklist maps malformed server date to unknown failure`() =
        runBlocking {
            val remoteDataSource =
                object : TeamChecklistRemoteDataSource {
                    override suspend fun getChecklist(): ApiResult<TeamChecklistResponseDto> =
                        ApiResult.Success(TeamChecklistResponseDto(date = "not-a-date"))

                    override suspend fun setItemChecked(
                        itemId: String,
                        request: UpdateChecklistItemRequestDto,
                    ): ApiResult<ChecklistItemStatusDto> = ApiResult.Failure(ApiError.Unknown)

                    override suspend fun getRounds(date: String): ApiResult<TeamChecklistRoundsResponseDto> =
                        ApiResult.Failure(ApiError.Unknown)

                    override suspend fun getRound(
                        roundId: String,
                        date: String,
                    ): ApiResult<TeamChecklistRoundDetailResponseDto> = ApiResult.Failure(ApiError.Unknown)

                    override suspend fun setRoundItemChecked(
                        roundId: String,
                        itemId: String,
                        request: UpdateChecklistRoundItemRequestDto,
                    ): ApiResult<ChecklistRoundItemStatusDto> = ApiResult.Failure(ApiError.Unknown)
                }

            val result = TeamChecklistRepositoryImpl(remoteDataSource).getChecklist()

            assertEquals(ApiResult.Failure(ApiError.Unknown), result)
        }
}
