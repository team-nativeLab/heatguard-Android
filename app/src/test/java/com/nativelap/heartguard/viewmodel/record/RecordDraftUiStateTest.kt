package com.nativelap.heartguard.viewmodel.record

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class RecordDraftUiStateTest {
    @Test
    fun `당일 휴식은 한국 오프셋과 선택 날짜를 유지한다`() {
        val draft = restDraft("13:00", "13:30")
        assertTrue(draft.hasValidRestTimeRange)
        assertEquals(30L, Duration.between(draft.restStartedAt, draft.restEndedAt).toMinutes())
        assertEquals(ZoneOffset.ofHours(9), draft.restStartedAt?.offset)
        assertEquals(LocalDate.of(2026, 10, 1), draft.restEndedAt?.toLocalDate())
    }

    @Test
    fun `종료 시간이 이르면 자정을 넘기고 같은 시간이면 1440분이다`() {
        val overnight = restDraft("23:50", "00:10")
        assertTrue(overnight.hasValidRestTimeRange)
        assertEquals(20L, Duration.between(overnight.restStartedAt, overnight.restEndedAt).toMinutes())
        assertEquals(LocalDate.of(2026, 10, 2), overnight.restEndedAt?.toLocalDate())
        val fullDay = restDraft("13:00", "13:00")
        assertTrue(fullDay.hasValidRestTimeRange)
        assertEquals(1440L, Duration.between(fullDay.restStartedAt, fullDay.restEndedAt).toMinutes())
    }

    @Test
    fun `날짜나 시작 또는 종료 입력이 없으면 저장 가능한 구간이 아니다`() {
        val draft = restDraft("13:00", "13:30")
        assertFalse(draft.copy(restDate = null).hasValidRestTimeRange)
        assertFalse(draft.copy(restStartTime = null).hasValidRestTimeRange)
        assertFalse(draft.copy(restEndTime = null).hasValidRestTimeRange)
    }

    private fun restDraft(
        start: String,
        end: String,
    ) = RecordDraftUiState(
        restDate = LocalDate.of(2026, 10, 1),
        restStartTime = LocalTime.parse(start),
        restEndTime = LocalTime.parse(end),
    )
}
