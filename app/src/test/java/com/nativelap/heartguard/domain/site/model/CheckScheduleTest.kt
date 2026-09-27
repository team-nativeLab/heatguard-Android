package com.nativelap.heartguard.domain.site.model

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CheckScheduleTest {
    @Test
    fun `다음 체크 시각과 남은 분을 계산한다`() {
        val checkSchedule = buildCheckSchedule(
            rawCheckTimes = listOf("11:00", "09:00", "13:00"),
            now = LocalTime.of(10, 3, 40),
        )

        assertEquals(
            listOf(LocalTime.of(9, 0), LocalTime.of(11, 0), LocalTime.of(13, 0)),
            checkSchedule.checkTimes,
        )
        assertEquals(LocalTime.of(11, 0), checkSchedule.nextCheckTime)
        assertEquals(57L, checkSchedule.minutesUntilNextCheck)
    }

    @Test
    fun `현재 시각과 같은 체크 시각은 다음 체크로 본다`() {
        val checkSchedule = buildCheckSchedule(
            rawCheckTimes = listOf("09:00"),
            now = LocalTime.of(9, 0, 30),
        )

        assertEquals(LocalTime.of(9, 0), checkSchedule.nextCheckTime)
        assertEquals(0L, checkSchedule.minutesUntilNextCheck)
    }

    @Test
    fun `남은 체크가 없으면 다음 체크는 null이다`() {
        val checkSchedule = buildCheckSchedule(
            rawCheckTimes = listOf("09:00", "11:00"),
            now = LocalTime.of(17, 0),
        )

        assertNull(checkSchedule.nextCheckTime)
        assertNull(checkSchedule.minutesUntilNextCheck)
    }

    @Test
    fun `형식이 잘못된 항목과 중복은 건너뛴다`() {
        val checkSchedule = buildCheckSchedule(
            rawCheckTimes = listOf("9시", "09:00", "09:00", "", "25:00"),
            now = LocalTime.of(8, 0),
        )

        assertEquals(listOf(LocalTime.of(9, 0)), checkSchedule.checkTimes)
    }
}
