package com.nativelap.heartguard.viewmodel.checklist

import com.nativelap.heartguard.domain.checklist.model.ChecklistRoundStatus
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChecklistRoundsRefreshTest {
    @Test
    fun `detail retry target survives when failed load has no detail`() {
        val retryTarget =
            resolveChecklistRoundRetryTarget(
                hasLoadError = true,
                isLoading = false,
                requestedRoundId = "08:00",
                requestedRoundDate = LocalDate.parse("2026-10-30"),
            )

        assertEquals("08:00", retryTarget?.roundId)
        assertEquals(LocalDate.parse("2026-10-30"), retryTarget?.date)
    }

    @Test
    fun `detail retry is not scheduled without an error or while loading`() {
        assertNull(
            resolveChecklistRoundRetryTarget(
                hasLoadError = false,
                isLoading = false,
                requestedRoundId = "08:00",
                requestedRoundDate = LocalDate.parse("2026-10-30"),
            ),
        )
        assertNull(
            resolveChecklistRoundRetryTarget(
                hasLoadError = true,
                isLoading = true,
                requestedRoundId = "08:00",
                requestedRoundDate = LocalDate.parse("2026-10-30"),
            ),
        )
    }

    @Test
    fun `edit gate follows Seoul local date and inclusive start exclusive end`() {
        val seoulZone = ZoneId.of("Asia/Seoul")
        val roundDate = LocalDate.parse("2026-10-30")
        val startsAt = OffsetDateTime.parse("2026-10-30T08:00:00+09:00")
        val endsAt = OffsetDateTime.parse("2026-10-30T10:00:00+09:00")
        val atStart = Clock.fixed(Instant.parse("2026-10-29T23:00:00Z"), seoulZone)
        val atEnd = Clock.fixed(Instant.parse("2026-10-30T01:00:00Z"), seoulZone)
        val nextSeoulDay = Clock.fixed(Instant.parse("2026-10-30T15:00:00Z"), seoulZone)

        assertTrue(isChecklistRoundEditable(roundDate, ChecklistRoundStatus.IN_PROGRESS, startsAt, endsAt, atStart))
        assertTrue(!isChecklistRoundEditable(roundDate, ChecklistRoundStatus.IN_PROGRESS, startsAt, endsAt, atEnd))
        assertTrue(
            !isChecklistRoundEditable(roundDate, ChecklistRoundStatus.IN_PROGRESS, startsAt, endsAt, nextSeoulDay),
        )
        assertTrue(!isChecklistRoundEditable(roundDate, ChecklistRoundStatus.SCHEDULED, startsAt, endsAt, atStart))
    }

    @Test
    fun `refresh wakes at round boundary and otherwise stays under sixty seconds`() {
        val now = Instant.parse("2026-10-29T23:59:00Z")
        val startsAt = Instant.parse("2026-10-30T00:00:00Z")

        assertEquals(60_000L, calculateRoundRefreshDelayMillis(now, startsAt, null))
        assertEquals(30_000L, calculateRoundRefreshDelayMillis(now, now.plusSeconds(30), null))
        assertEquals(60_000L, calculateRoundRefreshDelayMillis(now, null, now.minusSeconds(1)))
    }

    @Test
    fun `detail request generation prevents an older get from overwriting post-save refresh`() {
        val generation = ChecklistRequestGeneration()
        val delayedBeforeSave = generation.begin()

        generation.invalidate()
        val postSaveRefresh = generation.begin()
        var checkedValueInUi = false

        if (generation.isCurrent(postSaveRefresh)) {
            checkedValueInUi = true
        }
        if (generation.isCurrent(delayedBeforeSave)) {
            checkedValueInUi = false
        }

        assertTrue(checkedValueInUi)
        assertTrue(generation.isCurrent(postSaveRefresh))
        assertFalse(generation.isCurrent(delayedBeforeSave))
    }

    @Test
    fun `detail polling does not start another request while saving`() {
        assertFalse(shouldStartChecklistRoundDetailLoad(isSaving = true, isLoadingSameDetail = false))
        assertFalse(shouldStartChecklistRoundDetailLoad(isSaving = false, isLoadingSameDetail = true))
        assertTrue(shouldStartChecklistRoundDetailLoad(isSaving = false, isLoadingSameDetail = false))
        assertFalse(shouldStartChecklistRoundsLoad(isSaving = true))
        assertTrue(shouldStartChecklistRoundsLoad(isSaving = false))
    }

    @Test
    fun `pre-save rounds response cannot overwrite refreshed summary`() {
        val generation = ChecklistRequestGeneration()
        val delayedBeforeSave = generation.begin()

        generation.invalidate()
        val postSaveRefresh = generation.begin()
        var summary = "old"

        if (generation.isCurrent(postSaveRefresh)) {
            summary = "updated"
        }
        if (generation.isCurrent(delayedBeforeSave)) {
            summary = "stale"
        }

        assertEquals("updated", summary)
        assertFalse(generation.isCurrent(delayedBeforeSave))
        assertTrue(generation.isCurrent(postSaveRefresh))
    }
}
