package com.nativelap.heartguard.data.checklist.mapper

import com.nativelap.heartguard.data.checklist.dto.ChecklistItemDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.OffsetDateTime

class TeamChecklistMapperTest {
    @Test
    fun `서버 항목은 sortOrder와 체크 시간을 보존해 domain으로 변환한다`() {
        val response =
            TeamChecklistResponseDto(
                date = "2026-10-19",
                items =
                    listOf(
                        ChecklistItemDto(
                            itemId = "item_02",
                            text = "그늘에서 휴식했나요?",
                            sortOrder = 2,
                            checked = false,
                        ),
                        ChecklistItemDto(
                            itemId = "item_01",
                            text = "물을 마셨나요?",
                            sortOrder = 1,
                            checked = true,
                            checkedAt = "2026-10-19T09:10:00+09:00",
                        ),
                    ),
            )

        val checklist = response.toDomain()

        assertEquals(LocalDate.parse("2026-10-19"), checklist.date)
        assertEquals(listOf("item_01", "item_02"), checklist.items.map { item -> item.itemId })
        assertEquals("물을 마셨나요?", checklist.items.first().text)
        assertTrue(checklist.items.first().checked)
        assertEquals(
            OffsetDateTime.parse("2026-10-19T09:10:00+09:00"),
            checklist.items.first().checkedAt,
        )
        assertFalse(checklist.items.last().checked)
        assertEquals(null, checklist.items.last().checkedAt)
    }
}
