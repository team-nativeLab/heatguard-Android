package com.nativelap.heartguard.domain.record.model

const val MAX_RECORD_PHOTO_COUNT = 2

/** POST /api/v1/t/{teamToken}/records의 type 값이다. */
enum class FieldRecordType {
    THERMOMETER,
    WORK,
    REST,
}
