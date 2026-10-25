package com.nativelap.heartguard.domain.checklist.model

import java.time.LocalDate
import java.time.OffsetDateTime

data class TeamChecklist(
    val date: LocalDate,
    val items: List<TeamChecklistItem>,
)

data class TeamChecklistItem(
    val itemId: String,
    val text: String,
    val sortOrder: Int,
    val checked: Boolean,
    val checkedAt: OffsetDateTime?,
)

data class ChecklistItemCheckUpdate(
    val itemId: String,
    val checked: Boolean,
    val checkedAt: OffsetDateTime?,
)
