package com.nativelap.heartguard.data.checklist.mapper

import com.nativelap.heartguard.data.checklist.dto.ChecklistItemDto
import com.nativelap.heartguard.data.checklist.dto.ChecklistItemStatusDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistResponseDto
import com.nativelap.heartguard.domain.checklist.model.ChecklistItemCheckUpdate
import com.nativelap.heartguard.domain.checklist.model.TeamChecklist
import com.nativelap.heartguard.domain.checklist.model.TeamChecklistItem
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
