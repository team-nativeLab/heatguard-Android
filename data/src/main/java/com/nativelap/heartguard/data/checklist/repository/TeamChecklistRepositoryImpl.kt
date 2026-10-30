package com.nativelap.heartguard.data.checklist.repository

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.checklist.dto.UpdateChecklistItemRequestDto
import com.nativelap.heartguard.data.checklist.dto.UpdateChecklistRoundItemRequestDto
import com.nativelap.heartguard.data.checklist.mapper.toDomain
import com.nativelap.heartguard.data.checklist.remote.TeamChecklistRemoteDataSource
import com.nativelap.heartguard.domain.checklist.model.ChecklistItemCheckUpdate
import com.nativelap.heartguard.domain.checklist.model.ChecklistRoundItemCheckUpdate
import com.nativelap.heartguard.domain.checklist.model.TeamChecklist
import com.nativelap.heartguard.domain.checklist.model.TeamChecklistRoundDetail
import com.nativelap.heartguard.domain.checklist.model.TeamChecklistRounds
import com.nativelap.heartguard.domain.checklist.repository.TeamChecklistRepository
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject

class TeamChecklistRepositoryImpl
    @Inject
    constructor(
        private val teamChecklistRemoteDataSource: TeamChecklistRemoteDataSource,
    ) : TeamChecklistRepository {
        override suspend fun getChecklist(): ApiResult<TeamChecklist> {
            val response = teamChecklistRemoteDataSource.getChecklist()
            return when (response) {
                is ApiResult.Success -> {
                    try {
                        ApiResult.Success(response.value.toDomain())
                    } catch (_: DateTimeParseException) {
                        ApiResult.Failure(ApiError.Unknown)
                    }
                }

                is ApiResult.Failure -> {
                    response
                }
            }
        }

        override suspend fun setItemChecked(
            itemId: String,
            checked: Boolean,
        ): ApiResult<ChecklistItemCheckUpdate> =
            when (
                val updateResult =
                    teamChecklistRemoteDataSource.setItemChecked(
                        itemId = itemId,
                        request = UpdateChecklistItemRequestDto(checked = checked),
                    )
            ) {
                is ApiResult.Success -> ApiResult.Success(updateResult.value.toDomain())
                is ApiResult.Failure -> updateResult
            }

        override suspend fun getRounds(date: LocalDate): ApiResult<TeamChecklistRounds> =
            teamChecklistRemoteDataSource.getRounds(date.toString()).mapChecklistResult { response ->
                response.toDomain()
            }

        override suspend fun getRound(
            roundId: String,
            date: LocalDate,
        ): ApiResult<TeamChecklistRoundDetail> =
            teamChecklistRemoteDataSource.getRound(roundId, date.toString()).mapChecklistResult { response ->
                response.toDomain()
            }

        override suspend fun setRoundItemChecked(
            roundId: String,
            itemId: String,
            checked: Boolean,
        ): ApiResult<ChecklistRoundItemCheckUpdate> =
            teamChecklistRemoteDataSource
                .setRoundItemChecked(
                    roundId = roundId,
                    itemId = itemId,
                    request = UpdateChecklistRoundItemRequestDto(checked = checked),
                ).mapChecklistResult { response ->
                    response.toDomain()
                }
    }

private inline fun <Dto, Model> ApiResult<Dto>.mapChecklistResult(transform: (Dto) -> Model): ApiResult<Model> =
    when (this) {
        is ApiResult.Success -> {
            try {
                ApiResult.Success(transform(value))
            } catch (_: DateTimeParseException) {
                ApiResult.Failure(ApiError.Unknown)
            }
        }

        is ApiResult.Failure -> {
            this
        }
    }
