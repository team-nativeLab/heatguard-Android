package com.nativelap.heartguard.data.checklist.mapper

import com.nativelap.heartguard.data.checklist.dto.ChecklistItemDto
import com.nativelap.heartguard.data.checklist.dto.ChecklistItemStatusDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistResponseDto
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TeamChecklistMapperTest {
    @Test
    fun `toDomain maps server date and sorts items`() {
        val response =
            TeamChecklistResponseDto(
                date = "2026-10-28",
                items =
                    listOf(
                        ChecklistItemDto("second", "두 번째", 2, false),
                        ChecklistItemDto("first", "첫 번째", 1, true, "2026-10-28T09:30:00+09:00"),
                    ),
            )

        val checklist = response.toDomain()

        assertEquals(LocalDate.parse("2026-10-28"), checklist.date)
        assertEquals(listOf("first", "second"), checklist.items.map { it.itemId })
        assertEquals(OffsetDateTime.parse("2026-10-28T09:30:00+09:00"), checklist.items.first().checkedAt)
        assertNull(checklist.items.last().checkedAt)
    }

    @Test
    fun `toDomain maps empty checklist`() {
        val checklist = TeamChecklistResponseDto(date = "2026-10-28").toDomain()

        assertEquals(LocalDate.parse("2026-10-28"), checklist.date)
        assertEquals(emptyList(), checklist.items)
    }

    @Test
    fun `status response reflects saved checked value and valid time`() {
        val status =
            ChecklistItemStatusDto(
                itemId = "item-1",
                checked = true,
                checkedAt = "2026-10-28T10:00:00+09:00",
            ).toDomain()

        assertEquals("item-1", status.itemId)
        assertEquals(true, status.checked)
        assertEquals(OffsetDateTime.parse("2026-10-28T10:00:00+09:00"), status.checkedAt)
    }

    @Test
    fun `malformed checked time becomes null`() {
        val status = ChecklistItemStatusDto("item-1", checked = true, checkedAt = "invalid").toDomain()

        assertNull(status.checkedAt)
    }
}
