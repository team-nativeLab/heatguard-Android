package com.nativelap.heartguard.data.record.mapper

import com.nativelap.heartguard.data.record.dto.RecordHistoryItemDto
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.domain.record.model.RecordHistoryEntry
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

internal fun RecordHistoryItemDto.toDomain(): RecordHistoryEntry = RecordHistoryEntry(
    recordId = recordId,
    type = type.toFieldRecordTypeOrNull(),
    temperature = temperature,
    humidity = humidity,
    apparentTemperature = apparentTemperature,
    heatLevel = heatLevel,
    photoCount = maxOf(photoKeys.size, photoUrls.size),
    photoUrls = photoUrls,
    memo = memo?.takeIf { recordMemo -> recordMemo.isNotBlank() },
    measuredAt = measuredAt?.toOffsetDateTimeOrNull(),
)

// 서버가 앱이 모르는 유형을 보내도 목록 전체를 실패시키지 않고 null(알 수 없는 유형)로 둔다.
private fun String?.toFieldRecordTypeOrNull(): FieldRecordType? = when (this) {
    "THERMOMETER" -> FieldRecordType.THERMOMETER
    "WORK" -> FieldRecordType.WORK
    "REST" -> FieldRecordType.REST
    else -> null
}

private fun String.toOffsetDateTimeOrNull(): OffsetDateTime? {
    return try {
        OffsetDateTime.parse(this)
    } catch (_: DateTimeParseException) {
        null
    }
}
