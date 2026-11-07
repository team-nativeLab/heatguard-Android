package com.nativelap.heartguard.data.checklist.repository

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
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
import com.nativelap.heartguard.domain.site.repository.AppSiteLinkRepository
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject

class TeamChecklistRepositoryImpl
    @Inject
    constructor(
        private val teamChecklistRemoteDataSource: TeamChecklistRemoteDataSource,
        private val appSiteLinkRepository: AppSiteLinkRepository,
        private val sessionManager: SessionManager,
    ) : TeamChecklistRepository {
        override suspend fun getChecklist(): ApiResult<TeamChecklist> {
            if (!ensureSelectedTeam()) return ApiResult.Failure(ApiError.TeamNotSelected)
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
        ): ApiResult<ChecklistItemCheckUpdate> {
            if (!ensureSelectedTeam()) return ApiResult.Failure(ApiError.TeamNotSelected)
            return when (
                val updateResult =
                    teamChecklistRemoteDataSource.setItemChecked(
                        itemId = itemId,
                        request = UpdateChecklistItemRequestDto(checked = checked),
                    )
            ) {
                is ApiResult.Success -> ApiResult.Success(updateResult.value.toDomain())
                is ApiResult.Failure -> updateResult
            }
        }

        override suspend fun getRounds(date: LocalDate): ApiResult<TeamChecklistRounds> {
            if (!ensureSelectedTeam()) return ApiResult.Failure(ApiError.TeamNotSelected)
            return teamChecklistRemoteDataSource.getRounds(date.toString()).mapChecklistResult { response ->
                response.toDomain()
            }
        }

        override suspend fun getRound(
            roundId: String,
            date: LocalDate,
        ): ApiResult<TeamChecklistRoundDetail> {
            if (!ensureSelectedTeam()) return ApiResult.Failure(ApiError.TeamNotSelected)
            return teamChecklistRemoteDataSource.getRound(roundId, date.toString()).mapChecklistResult { response ->
                response.toDomain()
            }
        }

        override suspend fun setRoundItemChecked(
            roundId: String,
            itemId: String,
            checked: Boolean,
        ): ApiResult<ChecklistRoundItemCheckUpdate> {
            if (!ensureSelectedTeam()) return ApiResult.Failure(ApiError.TeamNotSelected)
            return teamChecklistRemoteDataSource
                .setRoundItemChecked(
                    roundId = roundId,
                    itemId = itemId,
                    request = UpdateChecklistRoundItemRequestDto(checked = checked),
                ).mapChecklistResult { response ->
                    response.toDomain()
                }
        }

        private suspend fun ensureSelectedTeam(): Boolean {
            val sessionSnapshot = sessionManager.getSnapshot()
            if (sessionManager.getSelectedTeamIdForSession(sessionSnapshot) != null) return true
            val refreshResult = appSiteLinkRepository.getLinkedSites(sessionSnapshot.generation)
            return refreshResult is ApiResult.Success &&
                sessionManager.getSelectedTeamIdForSession(sessionSnapshot) != null
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
