package com.nativelap.heartguard.domain.checklist.model

import java.time.LocalDate
import java.time.OffsetDateTime

enum class ChecklistRoundStatus {
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    MISSED,
    UNKNOWN,
}

data class TeamChecklistRounds(
    val date: LocalDate,
    val teamId: String,
    val summary: ChecklistRoundSummary,
    val items: List<TeamChecklistRoundSummary>,
)

data class ChecklistRoundSummary(
    val totalRounds: Int,
    val completedRounds: Int,
    val missedRounds: Int,
    val remainingRounds: Int,
    val progressPercent: Int,
)

data class TeamChecklistRoundSummary(
    val roundId: String,
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime,
    val status: ChecklistRoundStatus,
    val totalItems: Int,
    val checkedItems: Int,
    val progressPercent: Int,
    val completedAt: OffsetDateTime?,
)

data class TeamChecklistRoundDetail(
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
    val items: List<TeamChecklistRoundItem>,
)

data class TeamChecklistRoundItem(
    val itemId: String,
    val text: String,
    val sortOrder: Int,
    val checked: Boolean,
    val checkedAt: OffsetDateTime?,
)

data class ChecklistRoundItemCheckUpdate(
    val date: LocalDate,
    val roundId: String,
    val itemId: String,
    val checked: Boolean,
    val checkedAt: OffsetDateTime?,
)
