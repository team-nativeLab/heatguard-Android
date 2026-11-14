package com.nativelap.heartguard.data.site.local

import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherResponseDto
import com.nativelap.heartguard.domain.site.model.WeatherField
import com.nativelap.heartguard.domain.site.model.WeatherFieldProvenance
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal sealed interface WeatherCacheRead {
    data object Empty : WeatherCacheRead

    data object Invalid : WeatherCacheRead

    data class Valid(
        val cachedWeather: CachedCurrentLocationWeather,
    ) : WeatherCacheRead
}

internal fun readWeatherCacheFields(
    payload: String?,
    latitude: Double?,
    longitude: Double?,
    sessionOwner: String?,
    json: Json,
    fieldProvenancePayload: String? = null,
): WeatherCacheRead {
    if (payload == null && latitude == null && longitude == null && sessionOwner == null && fieldProvenancePayload == null) {
        return WeatherCacheRead.Empty
    }
    if (payload.isNullOrBlank() ||
        latitude == null || !latitude.isFinite() || latitude !in -90.0..90.0 ||
        longitude == null || !longitude.isFinite() || longitude !in -180.0..180.0 ||
        sessionOwner.isNullOrBlank()
    ) {
        return WeatherCacheRead.Invalid
    }

    val weather =
        try {
            json.decodeFromString<CurrentLocationWeatherResponseDto>(payload)
        } catch (_: SerializationException) {
            return WeatherCacheRead.Invalid
        } catch (_: IllegalArgumentException) {
            return WeatherCacheRead.Invalid
        }

    val fieldProvenance =
        fieldProvenancePayload?.let { payload ->
            try {
                json.decodeFromString<WeatherFieldProvenanceDto>(payload).toDomain()
            } catch (_: SerializationException) {
                return WeatherCacheRead.Invalid
            } catch (_: IllegalArgumentException) {
                return WeatherCacheRead.Invalid
            }
        } ?: weather.toInitialFieldProvenance()

    return WeatherCacheRead.Valid(
        CachedCurrentLocationWeather(
            latitude = latitude,
            longitude = longitude,
            weather = weather,
            sessionOwner = sessionOwner,
            fieldProvenance = fieldProvenance,
        ),
    )
}

internal suspend fun WeatherCacheRead.resolveOrClearInvalid(
    clearCache: suspend () -> Unit,
): CachedCurrentLocationWeather? =
    when (this) {
        WeatherCacheRead.Empty -> {
            null
        }

        WeatherCacheRead.Invalid -> {
            clearCache()
            null
        }

        is WeatherCacheRead.Valid -> {
            cachedWeather
        }
    }

@Serializable
internal data class WeatherFieldProvenanceDto(
    @SerialName("observedAtByField")
    val observedAtByField: Map<String, String> = emptyMap(),
    @SerialName("fieldsFromPreviousValue")
    val fieldsFromPreviousValue: Set<String> = emptySet(),
)

internal fun WeatherFieldProvenance.toCacheDto(): WeatherFieldProvenanceDto =
    WeatherFieldProvenanceDto(
        observedAtByField = observedAtByField.mapKeys { (field, _) -> field.name },
        fieldsFromPreviousValue = fieldsFromPreviousValue.mapTo(mutableSetOf()) { field -> field.name },
    )

private fun WeatherFieldProvenanceDto.toDomain(): WeatherFieldProvenance =
    WeatherFieldProvenance(
        observedAtByField =
            observedAtByField.mapNotNull { (fieldName, observedAt) ->
                WeatherField.entries
                    .firstOrNull { field -> field.name == fieldName }
                    ?.let { field -> field to observedAt }
            }.toMap(),
        fieldsFromPreviousValue =
            fieldsFromPreviousValue.mapNotNullTo(mutableSetOf()) { fieldName ->
                WeatherField.entries.firstOrNull { field -> field.name == fieldName }
            },
    )

internal fun CurrentLocationWeatherResponseDto.toInitialFieldProvenance(): WeatherFieldProvenance {
    val populatedFields =
        buildSet {
            if (temperature != null) add(WeatherField.TEMPERATURE)
            if (humidity != null) add(WeatherField.HUMIDITY)
            if (apparentTemperature != null) add(WeatherField.APPARENT_TEMPERATURE)
            if (heatLevel != null) add(WeatherField.HEAT_LEVEL)
            if (skyStatus != null) add(WeatherField.SKY_STATUS)
            if (temperatureDelta != null) add(WeatherField.TEMPERATURE_DELTA)
            if (coldWaveWarning != null) add(WeatherField.COLD_WAVE_WARNING)
        }
    val observedAtByField = observedAt?.let { timestamp -> populatedFields.associateWith { timestamp } }.orEmpty()
    return WeatherFieldProvenance(observedAtByField = observedAtByField)
}
