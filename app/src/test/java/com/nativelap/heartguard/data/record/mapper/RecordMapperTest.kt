package com.nativelap.heartguard.data.record.mapper

import com.nativelap.heartguard.data.record.dto.RecordResponseDto
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordMapperTest {

    @Test
    fun `RecordResponseDto가 도메인 모델로 그대로 매핑된다`() {
        val dto = RecordResponseDto(
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
    fun `FieldRecordType이 API 문자열 값으로 매핑된다`() {
        assertEquals("THERMOMETER", FieldRecordType.THERMOMETER.toApiValue())
        assertEquals("WORK", FieldRecordType.WORK.toApiValue())
        assertEquals("REST", FieldRecordType.REST.toApiValue())
    }
}
