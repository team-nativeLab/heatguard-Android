package com.nativelap.heartguard.data.record.mapper

import com.nativelap.heartguard.data.record.dto.RecordResponseDto
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.OffsetDateTime

class RecordMapperTest {
    @Test
    fun `RecordResponseDto가 도메인 모델로 그대로 매핑된다`() {
        val dto =
            RecordResponseDto(
                recordId = "rec_01",
                apparentTemperature = 31.2,
                heatLevel = 2,
                photoIds = listOf("photo_01"),
            )

        val record = dto.toDomain()

        assertEquals("rec_01", record.recordId)
        assertEquals(31.2, record.apparentTemperature)
        assertEquals(2, record.heatLevel)
        assertEquals(listOf("photo_01"), record.photoIds)
    }

    @Test
    fun `createdAt은 ISO 오프셋 시각이면 해석하고 없거나 형식이 다르면 null이다`() {
        val withCreatedAt =
            RecordResponseDto(
                recordId = "rec_01",
                createdAt = "2026-08-07T09:00:00+09:00",
            ).toDomain()
        val withoutCreatedAt = RecordResponseDto(recordId = "rec_02").toDomain()
        val invalidCreatedAt =
            RecordResponseDto(
                recordId = "rec_03",
                createdAt = "어제",
            ).toDomain()

        assertEquals(OffsetDateTime.parse("2026-08-07T09:00:00+09:00"), withCreatedAt.createdAt)
        assertNull(withoutCreatedAt.createdAt)
        assertNull(invalidCreatedAt.createdAt)
    }

    @Test
    fun `FieldRecordType이 API 문자열 값으로 매핑된다`() {
        assertEquals("THERMOMETER", FieldRecordType.THERMOMETER.toApiValue())
        assertEquals("WORK", FieldRecordType.WORK.toApiValue())
        assertEquals("REST", FieldRecordType.REST.toApiValue())
    }
}
