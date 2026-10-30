package com.nativelap.heartguard.data.checklist.mapper

import com.nativelap.heartguard.data.checklist.dto.ChecklistItemDto
import com.nativelap.heartguard.data.checklist.dto.ChecklistItemStatusDto
import com.nativelap.heartguard.data.checklist.dto.ChecklistRoundItemDto
import com.nativelap.heartguard.data.checklist.dto.ChecklistRoundItemStatusDto
import com.nativelap.heartguard.data.checklist.dto.ChecklistRoundSummaryDto
import com.nativelap.heartguard.data.checklist.dto.ChecklistRoundSummaryItemDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistResponseDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistRoundDetailResponseDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistRoundsResponseDto
import com.nativelap.heartguard.domain.checklist.model.ChecklistItemCheckUpdate
import com.nativelap.heartguard.domain.checklist.model.ChecklistRoundItemCheckUpdate
import com.nativelap.heartguard.domain.checklist.model.ChecklistRoundStatus
import com.nativelap.heartguard.domain.checklist.model.ChecklistRoundSummary
import com.nativelap.heartguard.domain.checklist.model.TeamChecklist
import com.nativelap.heartguard.domain.checklist.model.TeamChecklistItem
import com.nativelap.heartguard.domain.checklist.model.TeamChecklistRoundDetail
import com.nativelap.heartguard.domain.checklist.model.TeamChecklistRoundItem
import com.nativelap.heartguard.domain.checklist.model.TeamChecklistRoundSummary
import com.nativelap.heartguard.domain.checklist.model.TeamChecklistRounds
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

fun TeamChecklistResponseDto.toDomain(): TeamChecklist =
    TeamChecklist(
        date = LocalDate.parse(date),
        items = items.map(ChecklistItemDto::toDomain).sortedBy(TeamChecklistItem::sortOrder),
    )

fun ChecklistItemStatusDto.toDomain(): ChecklistItemCheckUpdate =
    ChecklistItemCheckUpdate(
        itemId = itemId,
        checked = checked,
        checkedAt = checkedAt.toOffsetDateTimeOrNull(),
    )

fun TeamChecklistRoundsResponseDto.toDomain(): TeamChecklistRounds =
    TeamChecklistRounds(
        date = LocalDate.parse(date),
        teamId = teamId,
        summary = summary.toDomain(),
        items = items.map(ChecklistRoundSummaryItemDto::toDomain),
    )

fun TeamChecklistRoundDetailResponseDto.toDomain(): TeamChecklistRoundDetail =
    TeamChecklistRoundDetail(
        date = LocalDate.parse(date),
        teamId = teamId,
        roundId = roundId,
        startsAt = OffsetDateTime.parse(startsAt),
        endsAt = OffsetDateTime.parse(endsAt),
        status = status.toDomain(),
        totalItems = totalItems,
        checkedItems = checkedItems,
        progressPercent = progressPercent,
        completedAt = completedAt.toOffsetDateTimeOrNull(),
        items = items.map(ChecklistRoundItemDto::toDomain).sortedBy(TeamChecklistRoundItem::sortOrder),
    )

fun ChecklistRoundItemStatusDto.toDomain(): ChecklistRoundItemCheckUpdate =
    ChecklistRoundItemCheckUpdate(
        date = LocalDate.parse(date),
        roundId = roundId,
        itemId = itemId,
        checked = checked,
        checkedAt = checkedAt.toOffsetDateTimeOrNull(),
    )

private fun ChecklistRoundSummaryDto.toDomain(): ChecklistRoundSummary =
    ChecklistRoundSummary(
        totalRounds = totalRounds,
        completedRounds = completedRounds,
        missedRounds = missedRounds,
        remainingRounds = remainingRounds,
        progressPercent = progressPercent,
    )

private fun ChecklistRoundSummaryItemDto.toDomain(): TeamChecklistRoundSummary =
    TeamChecklistRoundSummary(
        roundId = roundId,
        startsAt = OffsetDateTime.parse(startsAt),
        endsAt = OffsetDateTime.parse(endsAt),
        status = status.toDomain(),
        totalItems = totalItems,
        checkedItems = checkedItems,
        progressPercent = progressPercent,
        completedAt = completedAt.toOffsetDateTimeOrNull(),
    )

private fun ChecklistRoundItemDto.toDomain(): TeamChecklistRoundItem =
    TeamChecklistRoundItem(
        itemId = itemId,
        text = text,
        sortOrder = sortOrder,
        checked = checked,
        checkedAt = checkedAt.toOffsetDateTimeOrNull(),
    )

private fun String.toDomain(): ChecklistRoundStatus =
    when (this) {
        "SCHEDULED" -> ChecklistRoundStatus.SCHEDULED
        "IN_PROGRESS" -> ChecklistRoundStatus.IN_PROGRESS
        "COMPLETED" -> ChecklistRoundStatus.COMPLETED
        "MISSED" -> ChecklistRoundStatus.MISSED
        else -> ChecklistRoundStatus.UNKNOWN
    }

private fun ChecklistItemDto.toDomain(): TeamChecklistItem =
    TeamChecklistItem(
        itemId = itemId,
        text = text,
        sortOrder = sortOrder,
        checked = checked,
        checkedAt = checkedAt.toOffsetDateTimeOrNull(),
    )

private fun String?.toOffsetDateTimeOrNull(): OffsetDateTime? =
    this?.let { timestamp ->
        try {
            OffsetDateTime.parse(timestamp)
        } catch (_: DateTimeParseException) {
            null
        }
    }
