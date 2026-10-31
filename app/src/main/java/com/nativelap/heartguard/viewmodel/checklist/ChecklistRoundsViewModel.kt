package com.nativelap.heartguard.viewmodel.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.clearStateWhenSessionEnds
import com.nativelap.heartguard.domain.checklist.model.ChecklistRoundStatus
import com.nativelap.heartguard.domain.checklist.model.TeamChecklistRoundDetail
import com.nativelap.heartguard.domain.checklist.model.TeamChecklistRoundItem
import com.nativelap.heartguard.domain.checklist.model.TeamChecklistRounds
import com.nativelap.heartguard.domain.checklist.usecase.GetTeamChecklistRoundUseCase
import com.nativelap.heartguard.domain.checklist.usecase.GetTeamChecklistRoundsUseCase
import com.nativelap.heartguard.domain.checklist.usecase.SetTeamChecklistRoundItemCheckedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
internal class ChecklistRoundsViewModel
    @Inject
    constructor(
        private val getTeamChecklistRoundsUseCase: GetTeamChecklistRoundsUseCase,
        private val getTeamChecklistRoundUseCase: GetTeamChecklistRoundUseCase,
        private val setTeamChecklistRoundItemCheckedUseCase: SetTeamChecklistRoundItemCheckedUseCase,
        private val sessionManager: SessionManager,
        private val clock: Clock,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow(ChecklistRoundsUiState())
        val uiState = mutableUiState.asStateFlow()

        private var roundsLoadJob: Job? = null
        private var detailLoadJob: Job? = null
        private var saveJob: Job? = null
        private val roundsRequestGeneration = ChecklistRequestGeneration()
        private val detailRequestGeneration = ChecklistRequestGeneration()
        private var requestedRoundId: String? = null
        private var requestedRoundDate: LocalDate? = null
        private var ownedSessionGeneration = sessionManager.getSnapshot().generation

        init {
            clearStateWhenSessionEnds(sessionManager) {
                resetForSession(sessionManager.getSnapshot().generation)
            }
        }

        fun loadRounds(date: LocalDate = LocalDate.now(clock)) {
            val state = mutableUiState.value
            if (!shouldStartChecklistRoundsLoad(state.savingItemIds.isNotEmpty())) return
            if (state.isLoadingRounds && state.date == date) return

            roundsLoadJob?.cancel()
            val request = roundsRequestGeneration.begin()
            val sessionGeneration = sessionManager.getSnapshot().generation
            ownedSessionGeneration = sessionGeneration
            mutableUiState.value =
                state.copy(
                    date = date,
                    rounds = state.rounds.takeUnless { state.date != date },
                    isLoadingRounds = true,
                    hasRoundsLoadError = false,
                )
            roundsLoadJob =
                viewModelScope.launch {
                    when (val result = getTeamChecklistRoundsUseCase(date)) {
                        is ApiResult.Success -> {
                            if (isCurrentRoundsRequest(request, date, sessionGeneration)) {
                                mutableUiState.value =
                                    mutableUiState.value.copy(
                                        date = result.value.date,
                                        rounds = result.value.toUiModel(),
                                        isLoadingRounds = false,
                                        hasRoundsLoadError = false,
                                    )
                            }
                        }

                        is ApiResult.Failure -> {
                            if (isCurrentRoundsRequest(request, date, sessionGeneration)) {
                                mutableUiState.value =
                                    mutableUiState.value.copy(
                                        isLoadingRounds = false,
                                        hasRoundsLoadError = result.error != ApiError.SessionChanged,
                                    )
                            }
                        }
                    }
                }
        }

        fun loadRound(
            roundId: String,
            date: LocalDate,
        ) {
            val state = mutableUiState.value
            val isLoadingSameDetail =
                state.isLoadingDetail && requestedRoundId == roundId && requestedRoundDate == date
            if (!shouldStartChecklistRoundDetailLoad(state.savingItemIds.isNotEmpty(), isLoadingSameDetail)) {
                return
            }
            detailLoadJob?.cancel()
            val request = detailRequestGeneration.begin()
            val sessionGeneration = sessionManager.getSnapshot().generation
            ownedSessionGeneration = sessionGeneration
            requestedRoundId = roundId
            requestedRoundDate = date
            val currentDetail = mutableUiState.value.roundDetail
            mutableUiState.value =
                mutableUiState.value.copy(
                    date = date,
                    roundDetail = currentDetail?.takeIf { it.roundId == roundId && it.date == date },
                    isLoadingDetail = true,
                    hasDetailLoadError = false,
                    hasSaveError = false,
                )
            detailLoadJob =
                viewModelScope.launch {
                    when (val result = getTeamChecklistRoundUseCase(roundId, date)) {
                        is ApiResult.Success -> {
                            if (isCurrentDetailRequest(request, roundId, date, sessionGeneration)) {
                                mutableUiState.value =
                                    mutableUiState.value.copy(
                                        roundDetail = result.value.toUiModel(),
                                        isLoadingDetail = false,
                                        hasDetailLoadError = false,
                                    )
                            }
                        }

                        is ApiResult.Failure -> {
                            if (isCurrentDetailRequest(request, roundId, date, sessionGeneration)) {
                                mutableUiState.value =
                                    mutableUiState.value.copy(
                                        isLoadingDetail = false,
                                        hasDetailLoadError = result.error != ApiError.SessionChanged,
                                    )
                            }
                        }
                    }
                }
        }

        fun retryRounds() {
            val date = mutableUiState.value.date ?: LocalDate.now(clock)
            if (mutableUiState.value.hasRoundsLoadError && !mutableUiState.value.isLoadingRounds) {
                loadRounds(date)
            }
        }

        fun retryRound() {
            val retryTarget =
                resolveChecklistRoundRetryTarget(
                    hasLoadError = mutableUiState.value.hasDetailLoadError,
                    isLoading = mutableUiState.value.isLoadingDetail,
                    requestedRoundId = requestedRoundId,
                    requestedRoundDate = requestedRoundDate,
                ) ?: return
            loadRound(retryTarget.roundId, retryTarget.date)
        }

        fun nextRoundRefreshDelayMillis(
            roundId: String,
            date: LocalDate,
        ): Long {
            val currentInstant = clock.instant()
            val detail = mutableUiState.value.roundDetail?.takeIf { it.roundId == roundId && it.date == date }
            return calculateRoundRefreshDelayMillis(
                now = currentInstant,
                startsAt = detail?.startsAt?.toInstant(),
                endsAt = detail?.endsAt?.toInstant(),
            )
        }

        fun refreshIfDateChanged() {
            val currentDate = LocalDate.now(clock)
            if (mutableUiState.value.date != currentDate) loadRounds(currentDate)
        }

        fun setRoundItemChecked(
            roundId: String,
            date: LocalDate,
            itemId: String,
            isChecked: Boolean,
        ) {
            val state = mutableUiState.value
            val detail = state.roundDetail ?: return
            if (!detail.canEdit || detail.roundId != roundId || detail.date != date ||
                state.savingItemIds.isNotEmpty()
            ) {
                return
            }
            val item = detail.items.firstOrNull { it.itemId == itemId } ?: return
            if (item.isChecked == isChecked) return

            val sessionGeneration = sessionManager.getSnapshot().generation
            detailLoadJob?.cancel()
            detailRequestGeneration.invalidate()
            roundsLoadJob?.cancel()
            roundsRequestGeneration.invalidate()
            mutableUiState.value =
                state.copy(
                    savingItemIds = state.savingItemIds + itemId,
                    hasSaveError = false,
                    isLoadingDetail = false,
                    hasDetailLoadError = false,
                    isLoadingRounds = false,
                    hasRoundsLoadError = false,
                )
            saveJob =
                viewModelScope.launch {
                    try {
                        when (
                            val result =
                                setTeamChecklistRoundItemCheckedUseCase(
                                    roundId = roundId,
                                    itemId = itemId,
                                    checked = isChecked,
                                )
                        ) {
                            is ApiResult.Success -> {
                                if (isCurrentSession(sessionGeneration)) {
                                    applySavedItem(result.value.checked, result.value.checkedAt, itemId, roundId, date)
                                    refreshRoundAndList(roundId, date, sessionGeneration)
                                }
                            }

                            is ApiResult.Failure -> {
                                if (isCurrentSession(sessionGeneration)) {
                                    mutableUiState.value =
                                        mutableUiState.value.copy(
                                            hasSaveError = result.error != ApiError.SessionChanged,
                                        )
                                    if (result.error.isRoundNotOpen()) {
                                        refreshRoundAndList(roundId, date, sessionGeneration)
                                    }
                                }
                            }
                        }
                    } finally {
                        if (isCurrentSession(sessionGeneration)) {
                            mutableUiState.value =
                                mutableUiState.value.copy(
                                    savingItemIds = mutableUiState.value.savingItemIds - itemId,
                                )
                        }
                    }
                }
        }

        private suspend fun refreshRoundAndList(
            roundId: String,
            date: LocalDate,
            sessionGeneration: Long,
        ) {
            if (!isCurrentSession(sessionGeneration)) return

            val detailRequest = detailRequestGeneration.begin()
            val roundsDate = LocalDate.now(clock)
            val roundsRequest = roundsRequestGeneration.begin()
            mutableUiState.value =
                mutableUiState.value.copy(
                    date = roundsDate,
                    isLoadingDetail = true,
                    hasDetailLoadError = false,
                    isLoadingRounds = true,
                    hasRoundsLoadError = false,
                )

            coroutineScope {
                launch {
                    when (val detailResult = getTeamChecklistRoundUseCase(roundId, date)) {
                        is ApiResult.Success -> {
                            if (
                                detailRequestGeneration.isCurrent(detailRequest) &&
                                isCurrentSession(sessionGeneration) &&
                                requestedRoundId == roundId &&
                                requestedRoundDate == date
                            ) {
                                mutableUiState.value =
                                    mutableUiState.value.copy(
                                        roundDetail = detailResult.value.toUiModel(),
                                        isLoadingDetail = false,
                                        hasDetailLoadError = false,
                                    )
                            }
                        }

                        is ApiResult.Failure -> {
                            if (
                                detailRequestGeneration.isCurrent(detailRequest) &&
                                isCurrentSession(sessionGeneration)
                            ) {
                                mutableUiState.value =
                                    mutableUiState.value.copy(
                                        isLoadingDetail = false,
                                        hasDetailLoadError = detailResult.error != ApiError.SessionChanged,
                                    )
                            }
                        }
                    }
                }

                launch {
                    when (val roundsResult = getTeamChecklistRoundsUseCase(roundsDate)) {
                        is ApiResult.Success -> {
                            if (
                                roundsRequestGeneration.isCurrent(roundsRequest) &&
                                isCurrentSession(sessionGeneration)
                            ) {
                                mutableUiState.value =
                                    mutableUiState.value.copy(
                                        date = roundsResult.value.date,
                                        rounds = roundsResult.value.toUiModel(),
                                        isLoadingRounds = false,
                                        hasRoundsLoadError = false,
                                    )
                            }
                        }

                        is ApiResult.Failure -> {
                            if (
                                roundsRequestGeneration.isCurrent(roundsRequest) &&
                                isCurrentSession(sessionGeneration)
                            ) {
                                mutableUiState.value =
                                    mutableUiState.value.copy(
                                        isLoadingRounds = false,
                                        hasRoundsLoadError = roundsResult.error != ApiError.SessionChanged,
                                    )
                            }
                        }
                    }
                }
            }
        }

        private fun applySavedItem(
            isChecked: Boolean,
            checkedAt: java.time.OffsetDateTime?,
            itemId: String,
            roundId: String,
            date: LocalDate,
        ) {
            val detail = mutableUiState.value.roundDetail ?: return
            if (detail.roundId != roundId || detail.date != date) return
            val updatedItems =
                detail.items.map { row ->
                    if (row.itemId == itemId) {
                        row.copy(isChecked = isChecked, checkedAt = checkedAt)
                    } else {
                        row
                    }
                }
            mutableUiState.value =
                mutableUiState.value.copy(
                    roundDetail =
                        detail.copy(
                            checkedItems = updatedItems.count(ChecklistRoundItemUiModel::isChecked),
                            items = updatedItems,
                        ),
                )
        }

        private fun resetForSession(newSessionGeneration: Long) {
            roundsLoadJob?.cancel()
            detailLoadJob?.cancel()
            saveJob?.cancel()
            roundsRequestGeneration.invalidate()
            detailRequestGeneration.invalidate()
            requestedRoundId = null
            requestedRoundDate = null
            ownedSessionGeneration = newSessionGeneration
            mutableUiState.value = ChecklistRoundsUiState()
        }

        private fun isCurrentRoundsRequest(
            request: Long,
            date: LocalDate,
            sessionGeneration: Long,
        ): Boolean =
            shouldApplyChecklistRoundsResponse(
                requestDate = date,
                stateDate = mutableUiState.value.date,
                isLatestRequest = roundsRequestGeneration.isCurrent(request),
                isCurrentSession = isCurrentSession(sessionGeneration),
            )

        private fun isCurrentDetailRequest(
            request: Long,
            roundId: String,
            date: LocalDate,
            sessionGeneration: Long,
        ): Boolean =
            detailRequestGeneration.isCurrent(request) &&
                requestedRoundId == roundId &&
                requestedRoundDate == date &&
                isCurrentSession(sessionGeneration)

        private fun isCurrentSession(sessionGeneration: Long): Boolean =
            sessionGeneration == sessionManager.getSnapshot().generation &&
                sessionGeneration == ownedSessionGeneration

        private fun ApiError.isRoundNotOpen(): Boolean = this is ApiError.Http && errorCode == ROUND_NOT_OPEN_ERROR_CODE

        private fun TeamChecklistRounds.toUiModel() =
            ChecklistRoundsOverviewUiModel(
                date = date,
                teamId = teamId,
                totalRounds = summary.totalRounds,
                completedRounds = summary.completedRounds,
                missedRounds = summary.missedRounds,
                remainingRounds = summary.remainingRounds,
                progressPercent = summary.progressPercent,
                rounds =
                    items.map { round ->
                        ChecklistRoundCardUiModel(
                            roundId = round.roundId,
                            startsAt = round.startsAt,
                            endsAt = round.endsAt,
                            status = round.status,
                            totalItems = round.totalItems,
                            checkedItems = round.checkedItems,
                            progressPercent = round.progressPercent,
                            completedAt = round.completedAt,
                        )
                    },
            )

        private fun TeamChecklistRoundDetail.toUiModel(): ChecklistRoundDetailUiModel {
            val canEdit = isChecklistRoundEditable(date, status, startsAt, endsAt, clock)
            return ChecklistRoundDetailUiModel(
                date = date,
                teamId = teamId,
                roundId = roundId,
                startsAt = startsAt,
                endsAt = endsAt,
                status = status,
                totalItems = totalItems,
                checkedItems = checkedItems,
                progressPercent = progressPercent,
                completedAt = completedAt,
                canEdit = canEdit,
                items = items.map { item -> item.toUiModel() },
            )
        }

        private fun TeamChecklistRoundItem.toUiModel() =
            ChecklistRoundItemUiModel(
                itemId = itemId,
                title = text,
                isChecked = checked,
                checkedAt = checkedAt,
            )

        private companion object {
            const val ROUND_NOT_OPEN_ERROR_CODE = "ROUND_NOT_OPEN"
            const val MAX_ROUND_REFRESH_INTERVAL_MILLIS = 60_000L
        }
    }

internal data class ChecklistRoundRetryTarget(
    val roundId: String,
    val date: LocalDate,
)

internal fun resolveChecklistRoundRetryTarget(
    hasLoadError: Boolean,
    isLoading: Boolean,
    requestedRoundId: String?,
    requestedRoundDate: LocalDate?,
): ChecklistRoundRetryTarget? {
    if (!hasLoadError || isLoading) return null
    val roundId = requestedRoundId ?: return null
    val date = requestedRoundDate ?: return null
    return ChecklistRoundRetryTarget(roundId, date)
}

internal fun shouldApplyChecklistRoundsResponse(
    requestDate: LocalDate,
    stateDate: LocalDate?,
    isLatestRequest: Boolean,
    isCurrentSession: Boolean,
): Boolean = requestDate == stateDate && isLatestRequest && isCurrentSession

internal fun isChecklistRoundEditable(
    date: LocalDate,
    status: ChecklistRoundStatus,
    startsAt: java.time.OffsetDateTime,
    endsAt: java.time.OffsetDateTime,
    clock: Clock,
): Boolean {
    val now = clock.instant()
    return date == LocalDate.now(clock) &&
        status == ChecklistRoundStatus.IN_PROGRESS &&
        !now.isBefore(startsAt.toInstant()) &&
        now.isBefore(endsAt.toInstant())
}

internal fun calculateRoundRefreshDelayMillis(
    now: java.time.Instant,
    startsAt: java.time.Instant?,
    endsAt: java.time.Instant?,
): Long {
    val nextBoundary =
        when {
            startsAt != null && now.isBefore(startsAt) -> startsAt
            endsAt != null && now.isBefore(endsAt) -> endsAt
            else -> now.plusMillis(60_000L)
        }
    return Duration.between(now, nextBoundary).toMillis().coerceIn(1_000L, 60_000L)
}
