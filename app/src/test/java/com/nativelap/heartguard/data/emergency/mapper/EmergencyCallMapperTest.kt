package com.nativelap.heartguard.data.emergency.mapper

import com.nativelap.heartguard.data.emergency.dto.EmergencyCallResponseDto
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmergencyCallMapperTest {

    @Test
    fun `status ACTIVE는 EmergencyCallState ACTIVE로 매핑된다`() {
        val dto = EmergencyCallResponseDto(callId = "call_01", status = "ACTIVE")

        val status = dto.toDomain()

        assertEquals(EmergencyCallState.ACTIVE, status.state)
        assertEquals("call_01", status.callId)
        assertNull(status.acknowledgedAt)
    }

    @Test
    fun `status ACKNOWLEDGED는 EmergencyCallState ACKNOWLEDGED로 매핑되고 acknowledgedAt을 보존한다`() {
        val dto = EmergencyCallResponseDto(
            callId = "call_01",
            status = "ACKNOWLEDGED",
            acknowledgedAt = "2026-08-07T10:15:00+09:00",
        )

        val status = dto.toDomain()

        assertEquals(EmergencyCallState.ACKNOWLEDGED, status.state)
        assertEquals("2026-08-07T10:15:00+09:00", status.acknowledgedAt)
    }

    @Test
    fun `알 수 없는 status 문자열은 NONE으로 매핑된다`() {
        val dto = EmergencyCallResponseDto(status = "UNKNOWN_STATUS")

        val status = dto.toDomain()

        assertEquals(EmergencyCallState.NONE, status.state)
    }
}
