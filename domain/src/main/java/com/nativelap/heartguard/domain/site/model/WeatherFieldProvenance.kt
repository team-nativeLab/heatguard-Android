package com.nativelap.heartguard.domain.site.model

/** 각 날씨 항목이 어느 관측 시각에서 왔는지와 이전 값 사용 여부를 나타낸다. */
data class WeatherFieldProvenance(
    val observedAtByField: Map<WeatherField, String> = emptyMap(),
    val fieldsFromPreviousValue: Set<WeatherField> = emptySet(),
) {
    fun observedAtFor(
        field: WeatherField,
        fallbackObservedAt: String?,
    ): String? = observedAtByField[field] ?: fallbackObservedAt

    fun isFromPreviousValue(field: WeatherField): Boolean = field in fieldsFromPreviousValue
}

enum class WeatherField {
    TEMPERATURE,
    HUMIDITY,
    APPARENT_TEMPERATURE,
    HEAT_LEVEL,
    SKY_STATUS,
    TEMPERATURE_DELTA,
    COLD_WAVE_WARNING,
}
