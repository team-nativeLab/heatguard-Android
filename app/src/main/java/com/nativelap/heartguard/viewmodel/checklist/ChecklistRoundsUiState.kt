package com.nativelap.heartguard.viewmodel.checklist

import com.nativelap.heartguard.domain.checklist.model.ChecklistRoundStatus
import java.time.LocalDate
import java.time.OffsetDateTime

internal data class ChecklistRoundsUiState(
    val date: LocalDate? = null,
    val rounds: ChecklistRoundsOverviewUiModel? = null,
    val isLoadingRounds: Boolean = false,
    val hasRoundsLoadError: Boolean = false,
    val roundDetail: ChecklistRoundDetailUiModel? = null,
    val isLoadingDetail: Boolean = false,
    val hasDetailLoadError: Boolean = false,
    val savingItemIds: Set<String> = emptySet(),
    val hasSaveError: Boolean = false,
)

internal data class ChecklistRoundsOverviewUiModel(
    val date: LocalDate,
    val teamId: String,
    val totalRounds: Int,
    val completedRounds: Int,
    val missedRounds: Int,
    val remainingRounds: Int,
    val progressPercent: Int,
    val rounds: List<ChecklistRoundCardUiModel>,
)

internal data class ChecklistRoundCardUiModel(
    val roundId: String,
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime,
    val status: ChecklistRoundStatus,
    val totalItems: Int,
    val checkedItems: Int,
    val progressPercent: Int,
    val completedAt: OffsetDateTime?,
)

internal data class ChecklistRoundDetailUiModel(
    val date: LocalDate,
    val teamId: String,
    val roundId: String,
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime,
    val status: ChecklistRoundStatus,
    val totalItems: Int,
    val checkedItems: Int,
    val progressPercent: Int,
    val completedAt: OffsetDateTime?,
    val canEdit: Boolean,
    val items: List<ChecklistRoundItemUiModel>,
)

internal data class ChecklistRoundItemUiModel(
    val itemId: String,
    val title: String,
    val isChecked: Boolean,
    val checkedAt: OffsetDateTime?,
)
