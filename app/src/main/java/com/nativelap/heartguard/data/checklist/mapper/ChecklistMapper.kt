package com.nativelap.heartguard.data.checklist.mapper

import com.nativelap.heartguard.data.checklist.dto.ChecklistItemUpdateResponseDto
import com.nativelap.heartguard.data.checklist.dto.ChecklistItemDto
import com.nativelap.heartguard.data.checklist.dto.ChecklistResponseDto
import com.nativelap.heartguard.domain.checklist.model.Checklist
import com.nativelap.heartguard.domain.checklist.model.ChecklistItem
import com.nativelap.heartguard.domain.checklist.model.ChecklistItemUpdate

internal fun ChecklistResponseDto.toDomain(): Checklist = Checklist(
    date = date,
    items = items.sortedBy(ChecklistItemDto::sortOrder).map { item ->
        ChecklistItem(
            itemId = item.itemId,
            text = item.text,
            sortOrder = item.sortOrder,
            checked = item.checked,
            checkedAt = item.checkedAt,
        )
    },
)

internal fun ChecklistItemUpdateResponseDto.toDomain(): ChecklistItemUpdate = ChecklistItemUpdate(
    itemId = itemId,
    checked = checked,
    checkedAt = checkedAt,
)
