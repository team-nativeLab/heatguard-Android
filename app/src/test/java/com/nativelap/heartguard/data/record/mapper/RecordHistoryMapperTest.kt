package com.nativelap.heartguard.data.record.mapper

import com.nativelap.heartguard.data.record.dto.RecordHistoryItemDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecordHistoryMapperTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `명세 v0_1의 휴식 시간과 기록 당시 위치·팀명을 매핑한다`() {
        val itemDto = json.decodeFromString<RecordHistoryItemDto>(
            """
            {"recordId":"rec_01","type":"REST","photoKeys":["k1"],"measuredAt":"2026-09-27T12:05:00+09:00",
            "restMinutes":20,"teamName":"홍길동 팀","workplace":"옥상 그늘막","siteName":"서울현장","version":1}
            """.trimIndent(),
        )

        val recordEntry = itemDto.toDomain()

        assertEquals(20, recordEntry.restMinutes)
        assertEquals("홍길동 팀", recordEntry.teamName)
        assertEquals("옥상 그늘막", recordEntry.workplace)
        assertEquals("서울현장", recordEntry.siteName)
    }

    @Test
    fun `이전 기록처럼 위치·휴식 값이 null이거나 비어 있으면 null이다`() {
        val itemDto = json.decodeFromString<RecordHistoryItemDto>(
            """
            {"recordId":"rec_02","type":"WORK","restMinutes":null,"teamName":null,"workplace":"","siteName":null}
            """.trimIndent(),
        )

        val recordEntry = itemDto.toDomain()

        assertNull(recordEntry.restMinutes)
        assertNull(recordEntry.teamName)
        assertNull(recordEntry.workplace)
        assertNull(recordEntry.siteName)
    }
}
