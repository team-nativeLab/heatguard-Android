package com.nativelap.heartguard.data.record.mapper

import com.nativelap.heartguard.data.record.dto.RecordResponseDto
import com.nativelap.heartguard.domain.record.model.FieldRecord
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

internal fun RecordResponseDto.toDomain(): FieldRecord = FieldRecord(
    recordId = recordId,
    apparentTemperature = apparentTemperature,
    heatLevel = heatLevel,
    photoIds = photoIds,
    createdAt = createdAt?.toOffsetDateTimeOrNull(),
)

// ISO-8601 오프셋 시각(예: 2026-08-07T09:00:00+09:00)만 해석하고, 형식이 다르면 null로 둔다.
private fun String.toOffsetDateTimeOrNull(): OffsetDateTime? {
    return try {
        OffsetDateTime.parse(this)
    } catch (_: DateTimeParseException) {
        null
    }
}

internal fun FieldRecordType.toApiValue(): String = when (this) {
    FieldRecordType.THERMOMETER -> "THERMOMETER"
    FieldRecordType.WORK -> "WORK"
    FieldRecordType.REST -> "REST"
}
