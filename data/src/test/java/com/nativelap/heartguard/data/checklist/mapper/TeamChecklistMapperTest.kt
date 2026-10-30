package com.nativelap.heartguard.data.checklist.mapper

import com.nativelap.heartguard.data.checklist.dto.ChecklistItemDto
import com.nativelap.heartguard.data.checklist.dto.ChecklistItemStatusDto
import com.nativelap.heartguard.data.checklist.dto.ChecklistRoundItemDto
import com.nativelap.heartguard.data.checklist.dto.ChecklistRoundSummaryDto
import com.nativelap.heartguard.data.checklist.dto.ChecklistRoundSummaryItemDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistResponseDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistRoundDetailResponseDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistRoundsResponseDto
import com.nativelap.heartguard.domain.checklist.model.ChecklistRoundStatus
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TeamChecklistMapperTest {
    @Test
    fun `round list preserves server summary order and offset timestamps`() {
        val rounds =
            TeamChecklistRoundsResponseDto(
                date = "2026-10-30",
                teamId = "team_01",
                summary = ChecklistRoundSummaryDto(2, 1, 0, 1, 50),
                items =
                    listOf(
                        ChecklistRoundSummaryItemDto(
                            "08:00",
                            "2026-10-30T08:00:00+09:00",
                            "2026-10-30T10:00:00+09:00",
                            "COMPLETED",
                            6,
                            6,
                            100,
                            "2026-10-30T08:12:00+09:00",
                        ),
                        ChecklistRoundSummaryItemDto(
                            "10:00",
                            "2026-10-30T10:00:00+09:00",
                            "2026-10-30T12:00:00+09:00",
                            "UNRECOGNIZED",
                            6,
                            2,
                            33,
                        ),
                    ),
            ).toDomain()

        assertEquals(1, rounds.summary.completedRounds)
        assertEquals(listOf("08:00", "10:00"), rounds.items.map { it.roundId })
        assertEquals(ChecklistRoundStatus.COMPLETED, rounds.items.first().status)
        assertEquals(ChecklistRoundStatus.UNKNOWN, rounds.items.last().status)
        assertEquals(OffsetDateTime.parse("2026-10-30T08:12:00+09:00"), rounds.items.first().completedAt)
    }

    @Test
    fun `round detail sorts items and keeps read timestamps with offsets`() {
        val detail =
            TeamChecklistRoundDetailResponseDto(
                date = "2026-10-30",
                teamId = "team_01",
                roundId = "08:00",
                startsAt = "2026-10-30T08:00:00+09:00",
                endsAt = "2026-10-30T10:00:00+09:00",
                status = "IN_PROGRESS",
                totalItems = 2,
                checkedItems = 1,
                progressPercent = 50,
                items =
                    listOf(
                        ChecklistRoundItemDto("later", "나중", 2, false),
                        ChecklistRoundItemDto("first", "먼저", 1, true, "2026-10-30T08:10:00+09:00"),
                    ),
            ).toDomain()

        assertEquals(ChecklistRoundStatus.IN_PROGRESS, detail.status)
        assertEquals(listOf("first", "later"), detail.items.map { it.itemId })
        assertEquals(OffsetDateTime.parse("2026-10-30T08:10:00+09:00"), detail.items.first().checkedAt)
    }

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
