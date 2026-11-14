package com.nativelap.heartguard.data.site.local

import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherResponseDto
import com.nativelap.heartguard.domain.site.model.WeatherField
import com.nativelap.heartguard.domain.site.model.WeatherFieldProvenance
import com.nativelap.heartguard.domain.site.model.isFreshWeatherTimestamp
import com.nativelap.heartguard.domain.site.model.toWeatherInstantOrNull
import java.time.Instant

internal data class CurrentLocationWeatherSnapshot(
    val weather: CurrentLocationWeatherResponseDto,
    val fieldProvenance: WeatherFieldProvenance,
)

internal fun CurrentLocationWeatherResponseDto.toWeatherSnapshot(
    fieldProvenance: WeatherFieldProvenance = toInitialFieldProvenance(),
): CurrentLocationWeatherSnapshot = CurrentLocationWeatherSnapshot(this, fieldProvenance)

internal fun CurrentLocationWeatherSnapshot.expireWeatherParts(now: Instant): CurrentLocationWeatherSnapshot {
    val originalWeather = weather
    val originalProvenance = fieldProvenance
    val freshFields = mutableMapOf<WeatherField, String>()

    fun fieldTime(field: WeatherField): String? =
        originalProvenance
            .observedAtFor(field, originalWeather.observedAt)
            ?.takeIf { timestamp -> timestamp.isFreshWeatherTimestamp(now) }

    val temperatureTime = fieldTime(WeatherField.TEMPERATURE).takeIf { originalWeather.temperature != null }
    val humidityTime = fieldTime(WeatherField.HUMIDITY).takeIf { originalWeather.humidity != null }
    val apparentTemperatureTime =
        fieldTime(WeatherField.APPARENT_TEMPERATURE).takeIf { originalWeather.apparentTemperature != null }
    val heatLevelTime = fieldTime(WeatherField.HEAT_LEVEL).takeIf { originalWeather.heatLevel != null }
    val skyStatusTime = fieldTime(WeatherField.SKY_STATUS).takeIf { originalWeather.skyStatus != null }
    val temperatureDeltaTime =
        fieldTime(WeatherField.TEMPERATURE_DELTA).takeIf { originalWeather.temperatureDelta != null }
    val coldWaveWarning =
        originalWeather.coldWaveWarning?.takeIf { warning -> warning.fetchedAt.isFreshWeatherTimestamp(now) }

    listOfNotNull(
        temperatureTime?.let { WeatherField.TEMPERATURE to it },
        humidityTime?.let { WeatherField.HUMIDITY to it },
        apparentTemperatureTime?.let { WeatherField.APPARENT_TEMPERATURE to it },
        heatLevelTime?.let { WeatherField.HEAT_LEVEL to it },
        skyStatusTime?.let { WeatherField.SKY_STATUS to it },
        temperatureDeltaTime?.let { WeatherField.TEMPERATURE_DELTA to it },
        coldWaveWarning?.fetchedAt?.let { WeatherField.COLD_WAVE_WARNING to it },
    ).forEach { (field, timestamp) -> freshFields[field] = timestamp }

    val hasFreshMainWeather = freshFields.keys.any { field -> field != WeatherField.COLD_WAVE_WARNING }
    val hasFreshData = hasFreshMainWeather || coldWaveWarning != null
    val sanitizedAvailability =
        when {
            !hasFreshData -> "UNAVAILABLE"
            originalWeather.availability == "CACHED" || !hasFreshMainWeather -> "CACHED"
            else -> "AVAILABLE"
        }
    val latestMainObservedAt =
        freshFields
            .filterKeys { field -> field != WeatherField.COLD_WAVE_WARNING }
            .values
            .maxByOrNull { timestamp -> timestamp.toWeatherInstantOrNull() ?: Instant.MIN }
    val nextWeather =
        originalWeather.copy(
            temperature = originalWeather.temperature.takeIf { temperatureTime != null },
            humidity = originalWeather.humidity.takeIf { humidityTime != null },
            apparentTemperature = originalWeather.apparentTemperature.takeIf { apparentTemperatureTime != null },
            heatLevel = originalWeather.heatLevel.takeIf { heatLevelTime != null },
            observedAt = latestMainObservedAt,
            skyStatus = originalWeather.skyStatus.takeIf { skyStatusTime != null },
            temperatureDelta = originalWeather.temperatureDelta.takeIf { temperatureDeltaTime != null },
            availability = sanitizedAvailability,
            coldWaveWarning = coldWaveWarning,
        )
    val retainedFields = originalProvenance.fieldsFromPreviousValue.intersect(freshFields.keys)
    return CurrentLocationWeatherSnapshot(
        weather = nextWeather,
        fieldProvenance =
            WeatherFieldProvenance(
                observedAtByField = freshFields,
                fieldsFromPreviousValue = retainedFields,
            ),
    )
}

internal fun CurrentLocationWeatherSnapshot.hasFreshWeatherPart(now: Instant): Boolean =
    expireWeatherParts(now).let { snapshot ->
        snapshot.fieldProvenance.observedAtByField.isNotEmpty() ||
            snapshot.weather.coldWaveWarning
                ?.fetchedAt
                .isFreshWeatherTimestamp(now)
    }

internal fun CurrentLocationWeatherResponseDto.expireWeatherParts(now: Instant): CurrentLocationWeatherResponseDto {
    val isMainWeatherFresh = observedAt.isFreshWeatherTimestamp(now) && hasMainWeatherValue()
    val freshColdWaveWarning = coldWaveWarning?.takeIf { warning -> warning.fetchedAt.isFreshWeatherTimestamp(now) }
    val hasFreshData = isMainWeatherFresh || freshColdWaveWarning != null
    val sanitizedAvailability =
        when {
            !hasFreshData -> "UNAVAILABLE"
            availability == "CACHED" || !isMainWeatherFresh -> "CACHED"
            else -> "AVAILABLE"
        }
    return copy(
        temperature = temperature.takeIf { isMainWeatherFresh },
        humidity = humidity.takeIf { isMainWeatherFresh },
        apparentTemperature = apparentTemperature.takeIf { isMainWeatherFresh },
        heatLevel = heatLevel.takeIf { isMainWeatherFresh },
        observedAt = observedAt.takeIf { isMainWeatherFresh },
        skyStatus = skyStatus.takeIf { isMainWeatherFresh },
        temperatureDelta = temperatureDelta.takeIf { isMainWeatherFresh },
        availability = sanitizedAvailability,
        coldWaveWarning = freshColdWaveWarning,
    )
}

internal fun CurrentLocationWeatherResponseDto.hasFreshWeatherPart(now: Instant): Boolean =
    (observedAt.isFreshWeatherTimestamp(now) && hasMainWeatherValue()) ||
        coldWaveWarning?.fetchedAt.isFreshWeatherTimestamp(now)

internal fun CurrentLocationWeatherResponseDto.hasMainWeatherValue(): Boolean =
    temperature != null || humidity != null || apparentTemperature != null ||
        heatLevel != null || skyStatus != null || temperatureDelta != null

internal fun CurrentLocationWeatherResponseDto.mergeFreshWeather(
    previous: CurrentLocationWeatherSnapshot?,
    now: Instant,
): CurrentLocationWeatherSnapshot {
    val incomingSnapshot = toWeatherSnapshot().expireWeatherParts(now)
    val previousSnapshot = previous?.expireWeatherParts(now)
    val previousWeather = previousSnapshot?.weather
    val previousProvenance = previousSnapshot?.fieldProvenance
    val incomingWeather = incomingSnapshot.weather
    val incomingProvenance = incomingSnapshot.fieldProvenance
    val timestampsByField = mutableMapOf<WeatherField, String>()
    val fieldsFromPreviousValue = mutableSetOf<WeatherField>()

    fun <T> mergeField(
        field: WeatherField,
        incomingValue: T?,
        previousValue: T?,
    ): T? {
        val incomingTimestamp = incomingProvenance.observedAtByField[field]
        val previousTimestamp = previousProvenance?.observedAtByField?.get(field)
        val incomingIsOlder =
            incomingTimestamp != null && previousTimestamp != null &&
                (incomingTimestamp.toWeatherInstantOrNull() ?: Instant.MIN)
                    .isBefore(previousTimestamp.toWeatherInstantOrNull() ?: Instant.MIN)
        return if (incomingValue != null && !incomingIsOlder) {
            incomingTimestamp?.let { timestamp -> timestampsByField[field] = timestamp }
            incomingValue
        } else if (previousValue != null && previousTimestamp != null) {
            timestampsByField[field] = previousTimestamp
            fieldsFromPreviousValue += field
            previousValue
        } else {
            null
        }
    }

    val temperature =
        mergeField(
            WeatherField.TEMPERATURE,
            incomingWeather.temperature,
            previousWeather?.temperature,
        )
    val humidity =
        mergeField(
            WeatherField.HUMIDITY,
            incomingWeather.humidity,
            previousWeather?.humidity,
        )
    val apparentTemperature =
        mergeField(
            WeatherField.APPARENT_TEMPERATURE,
            incomingWeather.apparentTemperature,
            previousWeather?.apparentTemperature,
        )
    val heatLevel =
        mergeField(
            WeatherField.HEAT_LEVEL,
            incomingWeather.heatLevel,
            previousWeather?.heatLevel,
        )
    val skyStatus =
        mergeField(
            WeatherField.SKY_STATUS,
            incomingWeather.skyStatus,
            previousWeather?.skyStatus,
        )
    val temperatureDelta =
        mergeField(
            WeatherField.TEMPERATURE_DELTA,
            incomingWeather.temperatureDelta,
            previousWeather?.temperatureDelta,
        )
    val incomingWarning = incomingWeather.coldWaveWarning
    val previousWarning = previousWeather?.coldWaveWarning
    val incomingWarningTime = incomingWarning?.fetchedAt.toWeatherInstantOrNull()
    val previousWarningTime = previousWarning?.fetchedAt.toWeatherInstantOrNull()
    val keepPreviousWarning =
        previousWarning != null &&
            (
                incomingWarning == null || incomingWarning.availability !in setOf("AVAILABLE", "CACHED") ||
                    (
                        incomingWarningTime != null && previousWarningTime != null &&
                            incomingWarningTime.isBefore(previousWarningTime)
                    )
            )
    val selectedWarning =
        if (keepPreviousWarning) {
            previousWarning?.copy(availability = "CACHED", isStale = true)
        } else {
            incomingWarning
        }
    if (keepPreviousWarning && previousWarning?.fetchedAt != null) {
        timestampsByField[WeatherField.COLD_WAVE_WARNING] = previousWarning.fetchedAt
        fieldsFromPreviousValue += WeatherField.COLD_WAVE_WARNING
    } else if (selectedWarning?.fetchedAt != null) {
        timestampsByField[WeatherField.COLD_WAVE_WARNING] = selectedWarning.fetchedAt
    }
    val latestMainObservedAt =
        timestampsByField
            .filterKeys { field -> field != WeatherField.COLD_WAVE_WARNING }
            .values
            .maxByOrNull { timestamp -> timestamp.toWeatherInstantOrNull() ?: Instant.MIN }
    val hasIncomingMainField =
        timestampsByField.any { (field, timestamp) ->
            field != WeatherField.COLD_WAVE_WARNING &&
                field !in fieldsFromPreviousValue &&
                timestamp == incomingProvenance.observedAtByField[field]
        }
    val hasPreviousMainField = fieldsFromPreviousValue.any { field -> field != WeatherField.COLD_WAVE_WARNING }
    val availability =
        when {
            hasIncomingMainField -> incomingWeather.availability
            hasPreviousMainField -> "CACHED"
            else -> incomingWeather.availability
        }
    val mergedWeather =
        incomingWeather.copy(
            temperature = temperature,
            humidity = humidity,
            apparentTemperature = apparentTemperature,
            heatLevel = heatLevel,
            observedAt = latestMainObservedAt,
            skyStatus = skyStatus,
            temperatureDelta = temperatureDelta,
            availability = availability,
            coldWaveWarning = selectedWarning,
        )
    return CurrentLocationWeatherSnapshot(
        weather = mergedWeather,
        fieldProvenance =
            WeatherFieldProvenance(
                observedAtByField = timestampsByField,
                fieldsFromPreviousValue = fieldsFromPreviousValue,
            ),
    )
}
