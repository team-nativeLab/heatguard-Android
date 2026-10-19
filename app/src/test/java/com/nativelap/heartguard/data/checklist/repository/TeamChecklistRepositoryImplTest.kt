package com.nativelap.heartguard.data.checklist.repository

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.checklist.dto.ChecklistItemStatusDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistResponseDto
import com.nativelap.heartguard.data.checklist.dto.UpdateChecklistItemRequestDto
import com.nativelap.heartguard.data.checklist.remote.TeamChecklistRemoteDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TeamChecklistRepositoryImplTest {
    @Test
    fun `조회는 yyyy-MM-dd 날짜를 원격 계층에 전달하고 domain 결과를 반환한다`() =
        runTest {
            val remoteDataSource = FakeTeamChecklistRemoteDataSource()
            val repository = TeamChecklistRepositoryImpl(remoteDataSource)

            val result = repository.getChecklist(LocalDate.parse("2026-10-19"))

            assertEquals("2026-10-19", remoteDataSource.requestedDate)
            assertEquals(LocalDate.parse("2026-10-19"), (result as ApiResult.Success).value.date)
        }

    @Test
    fun `item inactive 오류는 domain 경계를 지나 그대로 전달한다`() =
        runTest {
            val repository =
                TeamChecklistRepositoryImpl(
                    FakeTeamChecklistRemoteDataSource(
                        updateResult = ApiResult.Failure(ApiError.Http(statusCode = 409, errorCode = "ITEM_INACTIVE")),
                    ),
                )

            val result = repository.setItemChecked(itemId = "item_01", checked = true)

            assertEquals(
                ApiResult.Failure(ApiError.Http(statusCode = 409, errorCode = "ITEM_INACTIVE")),
                result,
            )
        }

    private class FakeTeamChecklistRemoteDataSource(
        private val updateResult: ApiResult<ChecklistItemStatusDto> =
            ApiResult.Success(ChecklistItemStatusDto(itemId = "item_01", checked = true)),
    ) : TeamChecklistRemoteDataSource {
        var requestedDate: String? = null

        override suspend fun getChecklist(date: String): ApiResult<TeamChecklistResponseDto> {
            requestedDate = date
            return ApiResult.Success(TeamChecklistResponseDto(date = date, items = emptyList()))
        }

        override suspend fun setItemChecked(
            itemId: String,
            request: UpdateChecklistItemRequestDto,
        ): ApiResult<ChecklistItemStatusDto> = updateResult
    }
}
