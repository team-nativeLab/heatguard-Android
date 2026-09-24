package com.nativelap.heartguard.data.record.mapper

import com.nativelap.heartguard.data.record.dto.RecordResponseDto
import com.nativelap.heartguard.domain.record.model.FieldRecord
import com.nativelap.heartguard.domain.record.model.FieldRecordType

internal fun RecordResponseDto.toDomain(): FieldRecord = FieldRecord(
    recordId = recordId,
    apparentTemperature = apparentTemperature,
    heatLevel = heatLevel,
    photoIds = photoIds,
)

internal fun FieldRecordType.toApiValue(): String = when (this) {
    FieldRecordType.THERMOMETER -> "THERMOMETER"
    FieldRecordType.WORK -> "WORK"
    FieldRecordType.REST -> "REST"
}
