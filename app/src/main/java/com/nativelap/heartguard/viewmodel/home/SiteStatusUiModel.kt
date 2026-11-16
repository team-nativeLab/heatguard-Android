package com.nativelap.heartguard.viewmodel.home

import androidx.compose.runtime.Immutable
import com.nativelap.heartguard.domain.site.model.ColdWaveAvailability
import com.nativelap.heartguard.domain.site.model.ColdWaveLevel
import com.nativelap.heartguard.domain.site.model.ColdWaveWarning
import com.nativelap.heartguard.domain.site.model.CurrentLocationWeather
import com.nativelap.heartguard.domain.site.model.SkyStatus
import com.nativelap.heartguard.domain.site.model.TeamSiteOverview
import com.nativelap.heartguard.domain.site.model.WeatherAvailability
import com.nativelap.heartguard.domain.site.model.WeatherField
import com.nativelap.heartguard.domain.site.model.WeatherFieldProvenance
import com.nativelap.heartguard.domain.site.model.elapsedWeatherMinutes
import com.nativelap.heartguard.domain.site.model.isFreshWeatherTimestamp
import com.nativelap.heartguard.domain.site.model.toWeatherInstantOrNull
import com.nativelap.heartguard.viewmodel.toDisplayNumber
import java.time.Instant

/** 팀 현장페이지 응답 중 여러 화면(홈·온도계 기록·현장 사진·긴급 호출)이 함께 보여주는 값이다.
 * 서버 응답을 아직 받지 못했거나 실패했으면 각 값이 null이며, 화면은 이를 "--"로 표시한다.
 * 숫자는 단위 없이 표시용 문자열로만 바꿔 두고, 단위·문구 조합은 화면의 문자열 리소스가 맡는다. */
@Immutable
data class SiteStatusUiModel(
    val temperature: String? = null,
    val humidity: String? = null,
    val apparentTemperature: String? = null,
    val heatLevel: Int? = null,
    val managerPhoneNumber: String? = null,
    val teamName: String? = null,
    val workplace: String? = null,
    val headquartersPhoneNumber: String? = null,
    val skyStatus: SkyStatus? = null,
    val temperatureDelta: String? = null,
    val isTemperatureIncreasing: Boolean? = null,
    val isTemperatureDeltaZero: Boolean = false,
    val recordTemperature: Double? = null,
    val recordHumidity: Double? = null,
    val coldWaveWarning: ColdWaveWarning? = null,
    val isColdWaveWarningUnavailable: Boolean = false,
    val weatherObservedAt: String? = null,
    val locationWeatherObservedAt: String? = null,
    val hasLocationWeatherValue: Boolean = false,
    val isLocationWeatherCached: Boolean = false,
    val isLastLocationWeather: Boolean = false,
    val weatherFieldsFromPreviousValue: Set<WeatherField> = emptySet(),
    val coldWaveFetchedAt: String? = null,
    val weatherAgeMinutes: Long? = null,
    val coldWaveAgeMinutes: Long? = null,
    val isLocationWeatherExpired: Boolean = false,
)

enum class HomeRiskBadgeType {
    HEAT,
    COLD_WAVE_ADVISORY,
    COLD_WAVE_WARNING,
}

fun SiteStatusUiModel.toHomeRiskBadgeType(): HomeRiskBadgeType =
    when (coldWaveWarning.confirmedForHome()?.level) {
        ColdWaveLevel.ADVISORY -> HomeRiskBadgeType.COLD_WAVE_ADVISORY
        ColdWaveLevel.WARNING -> HomeRiskBadgeType.COLD_WAVE_WARNING
        else -> HomeRiskBadgeType.HEAT
    }

/** 홈 화면 상태와 현재 위치 날씨를 공용 현장 상태로 바꾼다. */
fun HomeUiState.toSiteStatusUiModel(
    locationWeather: CurrentLocationWeather? = null,
    now: Instant = Instant.now(),
    lastLocationWeather: CurrentLocationWeather? = null,
): SiteStatusUiModel {
    val siteStatus =
        (this as? HomeUiState.Success)
            ?.overview
            ?.toSiteStatusUiModel(now)
            ?: SiteStatusUiModel()
    val isLastLocationWeather = locationWeather == null && lastLocationWeather != null
    val availableLocationWeather =
        (locationWeather ?: lastLocationWeather)?.takeIf { weather ->
            weather.availability == WeatherAvailability.AVAILABLE ||
                weather.availability == WeatherAvailability.CACHED
        } ?: return siteStatus
    val isTemperatureDisplayable =
        availableLocationWeather.temperature.isNotNullAnd {
            availableLocationWeather.isFieldDisplayable(WeatherField.TEMPERATURE, now)
        }
    val isHumidityDisplayable =
        availableLocationWeather.humidity.isNotNullAnd {
            availableLocationWeather.isFieldDisplayable(WeatherField.HUMIDITY, now)
        }
    val isApparentTemperatureDisplayable =
        availableLocationWeather.apparentTemperature.isNotNullAnd {
            availableLocationWeather.isFieldDisplayable(WeatherField.APPARENT_TEMPERATURE, now)
        }
    val isHeatLevelDisplayable =
        availableLocationWeather.heatLevel.isNotNullAnd {
            availableLocationWeather.isFieldDisplayable(WeatherField.HEAT_LEVEL, now)
        }
    val isSkyStatusDisplayable =
        availableLocationWeather.skyStatus.isNotNullAnd {
            availableLocationWeather.isFieldDisplayable(WeatherField.SKY_STATUS, now)
        }
    val isTemperatureDeltaDisplayable =
        availableLocationWeather.temperatureDelta.isNotNullAnd {
            availableLocationWeather.isFieldDisplayable(WeatherField.TEMPERATURE_DELTA, now)
        }
    val isMainWeatherDisplayable =
        isTemperatureDisplayable || isHumidityDisplayable || isApparentTemperatureDisplayable ||
            isHeatLevelDisplayable || isSkyStatusDisplayable || isTemperatureDeltaDisplayable
    val freshLocationColdWaveWarning =
        availableLocationWeather.coldWaveWarning
            ?.takeIf { warning -> warning.fetchedAt.isFreshWeatherTimestamp(now) }

    val selectedColdWaveWarning =
        freshLocationColdWaveWarning.confirmedForHome()
            ?: siteStatus.coldWaveWarning.confirmedForHome()
    val hasFreshSelectedColdWaveWarning = selectedColdWaveWarning != null
    val replacedWeatherFields =
        buildSet {
            if (isTemperatureDisplayable) add(WeatherField.TEMPERATURE)
            if (isHumidityDisplayable) add(WeatherField.HUMIDITY)
            if (isApparentTemperatureDisplayable) add(WeatherField.APPARENT_TEMPERATURE)
            if (isHeatLevelDisplayable) add(WeatherField.HEAT_LEVEL)
            if (isSkyStatusDisplayable) add(WeatherField.SKY_STATUS)
            if (isTemperatureDeltaDisplayable) add(WeatherField.TEMPERATURE_DELTA)
        }
    val locationFieldsFromPrevious =
        availableLocationWeather.fieldProvenance.fieldsFromPreviousValue.intersect(replacedWeatherFields)

    return siteStatus.copy(
        temperature =
            if (isTemperatureDisplayable) {
                availableLocationWeather.temperature.toDisplayNumber()
            } else {
                siteStatus.temperature
            },
        humidity =
            if (isHumidityDisplayable) {
                availableLocationWeather.humidity.toDisplayNumber()
            } else {
                siteStatus.humidity
            },
        apparentTemperature =
            if (isApparentTemperatureDisplayable) {
                availableLocationWeather.apparentTemperature.toDisplayNumber()
            } else {
                siteStatus.apparentTemperature
            },
        heatLevel = if (isHeatLevelDisplayable) availableLocationWeather.heatLevel else siteStatus.heatLevel,
        skyStatus = if (isSkyStatusDisplayable) availableLocationWeather.skyStatus else siteStatus.skyStatus,
        temperatureDelta =
            if (isTemperatureDeltaDisplayable) {
                availableLocationWeather.temperatureDelta?.toSignedDisplayNumber()
            } else {
                siteStatus.temperatureDelta
            },
        isTemperatureIncreasing =
            if (isTemperatureDeltaDisplayable) {
                availableLocationWeather.temperatureDelta?.toIncreasingDirection()
            } else {
                siteStatus.isTemperatureIncreasing
            },
        isTemperatureDeltaZero =
            if (isTemperatureDeltaDisplayable) {
                availableLocationWeather.temperatureDelta == 0.0
            } else {
                siteStatus.isTemperatureDeltaZero
            },
        recordTemperature =
            if (!isLastLocationWeather && availableLocationWeather.isFieldFresh(WeatherField.TEMPERATURE, now)) {
                availableLocationWeather.temperature
            } else {
                siteStatus.recordTemperature
            },
        recordHumidity =
            if (!isLastLocationWeather && availableLocationWeather.isFieldFresh(WeatherField.HUMIDITY, now)) {
                availableLocationWeather.humidity
            } else {
                siteStatus.recordHumidity
            },
        coldWaveWarning = selectedColdWaveWarning,
        weatherObservedAt =
            if (isMainWeatherDisplayable) availableLocationWeather.observedAt else siteStatus.weatherObservedAt,
        locationWeatherObservedAt =
            availableLocationWeather.observedAt.takeIf { isMainWeatherDisplayable },
        hasLocationWeatherValue = isMainWeatherDisplayable,
        isLocationWeatherCached =
            isMainWeatherDisplayable && availableLocationWeather.availability == WeatherAvailability.CACHED,
        isLastLocationWeather = isLastLocationWeather && isMainWeatherDisplayable,
        weatherFieldsFromPreviousValue =
            (siteStatus.weatherFieldsFromPreviousValue - replacedWeatherFields) + locationFieldsFromPrevious,
        coldWaveFetchedAt = selectedColdWaveWarning?.fetchedAt,
        weatherAgeMinutes = availableLocationWeather.observedAt.elapsedWeatherMinutes(now),
        coldWaveAgeMinutes = selectedColdWaveWarning?.fetchedAt.elapsedWeatherMinutes(now),
        isLocationWeatherExpired = !isMainWeatherDisplayable,
        isColdWaveWarningUnavailable =
            !hasFreshSelectedColdWaveWarning &&
                (
                    availableLocationWeather.coldWaveWarning?.availability == ColdWaveAvailability.UNAVAILABLE ||
                        (availableLocationWeather.coldWaveWarning != null && freshLocationColdWaveWarning == null) ||
                        siteStatus.isColdWaveWarningUnavailable
                ),
    )
}

private fun TeamSiteOverview.toSiteStatusUiModel(now: Instant): SiteStatusUiModel {
    val temperatureIsFresh =
        currentTemperature != null &&
            weatherFieldProvenance.observedAtFor(WeatherField.TEMPERATURE, observedAt).isFreshWeatherTimestamp(now)
    val humidityIsFresh =
        humidity != null &&
            weatherFieldProvenance.observedAtFor(WeatherField.HUMIDITY, observedAt).isFreshWeatherTimestamp(now)
    val apparentTemperatureIsFresh =
        apparentTemperature != null &&
            weatherFieldProvenance
                .observedAtFor(WeatherField.APPARENT_TEMPERATURE, observedAt)
                .isFreshWeatherTimestamp(now)
    val heatLevelIsFresh =
        heatLevel != null &&
            weatherFieldProvenance.observedAtFor(WeatherField.HEAT_LEVEL, observedAt).isFreshWeatherTimestamp(now)
    val skyStatusIsFresh =
        skyStatus != null &&
            weatherFieldProvenance.observedAtFor(WeatherField.SKY_STATUS, observedAt).isFreshWeatherTimestamp(now)
    val temperatureDeltaIsFresh =
        temperatureDelta != null &&
            weatherFieldProvenance
                .observedAtFor(WeatherField.TEMPERATURE_DELTA, observedAt)
                .isFreshWeatherTimestamp(now)
    val freshColdWaveWarning = coldWaveWarning?.takeIf { warning -> warning.fetchedAt.isFreshWeatherTimestamp(now) }
    return SiteStatusUiModel(
        temperature = currentTemperature.takeIf { temperatureIsFresh }.toDisplayNumber(),
        humidity = humidity.takeIf { humidityIsFresh }.toDisplayNumber(),
        apparentTemperature = apparentTemperature.takeIf { apparentTemperatureIsFresh }.toDisplayNumber(),
        heatLevel = heatLevel.takeIf { heatLevelIsFresh },
        managerPhoneNumber = managerPhoneNumber,
        teamName = teamName,
        workplace = workplace,
        headquartersPhoneNumber = headquartersPhoneNumber,
        skyStatus = skyStatus.takeIf { skyStatusIsFresh },
        temperatureDelta = temperatureDelta.takeIf { temperatureDeltaIsFresh }?.toSignedDisplayNumber(),
        isTemperatureIncreasing = temperatureDelta.takeIf { temperatureDeltaIsFresh }?.toIncreasingDirection(),
        isTemperatureDeltaZero = temperatureDeltaIsFresh && temperatureDelta == 0.0,
        recordTemperature = currentTemperature.takeIf { temperatureIsFresh },
        recordHumidity = humidity.takeIf { humidityIsFresh },
        coldWaveWarning = freshColdWaveWarning,
        isColdWaveWarningUnavailable = coldWaveWarning != null && freshColdWaveWarning == null,
        weatherObservedAt =
            observedAt.takeIf {
                temperatureIsFresh || humidityIsFresh || apparentTemperatureIsFresh ||
                    heatLevelIsFresh || skyStatusIsFresh || temperatureDeltaIsFresh
            },
        coldWaveFetchedAt = freshColdWaveWarning?.fetchedAt,
        weatherAgeMinutes = observedAt.elapsedWeatherMinutes(now),
        coldWaveAgeMinutes = freshColdWaveWarning?.fetchedAt.elapsedWeatherMinutes(now),
        weatherFieldsFromPreviousValue =
            weatherFieldProvenance.fieldsFromPreviousValue.filterTo(mutableSetOf()) { field ->
                when (field) {
                    WeatherField.TEMPERATURE -> temperatureIsFresh
                    WeatherField.HUMIDITY -> humidityIsFresh
                    WeatherField.APPARENT_TEMPERATURE -> apparentTemperatureIsFresh
                    WeatherField.HEAT_LEVEL -> heatLevelIsFresh
                    WeatherField.SKY_STATUS -> skyStatusIsFresh
                    WeatherField.TEMPERATURE_DELTA -> temperatureDeltaIsFresh
                    WeatherField.COLD_WAVE_WARNING -> freshColdWaveWarning != null
                }
            },
    )
}

private inline fun <T> T?.isNotNullAnd(predicate: () -> Boolean): Boolean = this != null && predicate()

private fun CurrentLocationWeather.isFieldDisplayable(
    field: WeatherField,
    now: Instant,
): Boolean {
    val fieldTimestamp = fieldProvenance.observedAtFor(field, observedAt)
    val observation = fieldTimestamp.toWeatherInstantOrNull() ?: return false
    if (observation.isAfter(now)) return false
    if (fieldProvenance.isFromPreviousValue(field)) {
        return fieldTimestamp.isFreshWeatherTimestamp(now)
    }
    return fieldTimestamp.isFreshWeatherTimestamp(now) ||
        (availability == WeatherAvailability.AVAILABLE && serverReceivedAt.isFreshWeatherTimestamp(now))
}

private fun CurrentLocationWeather.isFieldFresh(
    field: WeatherField,
    now: Instant,
): Boolean = fieldProvenance.observedAtFor(field, observedAt).isFreshWeatherTimestamp(now)

internal fun TeamSiteOverview.mergeWeatherFieldsFrom(
    previous: TeamSiteOverview?,
    now: Instant,
): TeamSiteOverview {
    if (previous == null) return this
    val selectedTimestamps = mutableMapOf<WeatherField, String>()
    val fieldsFromPrevious = mutableSetOf<WeatherField>()

    fun <T> selectField(
        field: WeatherField,
        incomingValue: T?,
        previousValue: T?,
    ): T? {
        val selected =
            selectWeatherField(
                incomingValue,
                weatherFieldProvenance.observedAtFor(field, observedAt),
                previousValue,
                previous.weatherFieldProvenance.observedAtFor(field, previous.observedAt),
                now,
            )
        selected.timestamp?.let { timestamp -> selectedTimestamps[field] = timestamp }
        if (selected.fromPreviousValue) fieldsFromPrevious += field
        return selected.value
    }

    val mergedTemperature = selectField(WeatherField.TEMPERATURE, currentTemperature, previous.currentTemperature)
    val mergedHumidity = selectField(WeatherField.HUMIDITY, humidity, previous.humidity)
    val mergedApparentTemperature =
        selectField(WeatherField.APPARENT_TEMPERATURE, apparentTemperature, previous.apparentTemperature)
    val mergedHeatLevel = selectField(WeatherField.HEAT_LEVEL, heatLevel, previous.heatLevel)
    val mergedSkyStatus = selectField(WeatherField.SKY_STATUS, skyStatus, previous.skyStatus)
    val mergedTemperatureDelta =
        selectField(WeatherField.TEMPERATURE_DELTA, temperatureDelta, previous.temperatureDelta)
    val incomingWarningTime = coldWaveWarning?.fetchedAt?.takeIf { timestamp -> timestamp.isFreshWeatherTimestamp(now) }
    val previousWarningTime =
        previous.coldWaveWarning?.fetchedAt?.takeIf { timestamp -> timestamp.isFreshWeatherTimestamp(now) }
    val keepPreviousWarning =
        previous.coldWaveWarning != null && previousWarningTime != null &&
            (
                coldWaveWarning == null || incomingWarningTime == null ||
                    (incomingWarningTime.toWeatherInstantOrNull() ?: Instant.MIN)
                        .isBefore(previousWarningTime.toWeatherInstantOrNull() ?: Instant.MIN)
            )
    val mergedColdWaveWarning = if (keepPreviousWarning) previous.coldWaveWarning else coldWaveWarning
    if (keepPreviousWarning) {
        selectedTimestamps[WeatherField.COLD_WAVE_WARNING] = previousWarningTime
        fieldsFromPrevious += WeatherField.COLD_WAVE_WARNING
    } else if (incomingWarningTime != null) {
        selectedTimestamps[WeatherField.COLD_WAVE_WARNING] = incomingWarningTime
    }
    val latestWeatherObservedAt =
        selectedTimestamps
            .filterKeys { field -> field != WeatherField.COLD_WAVE_WARNING }
            .values
            .maxByOrNull { timestamp -> timestamp.toWeatherInstantOrNull() ?: Instant.MIN }
    return copy(
        currentTemperature = mergedTemperature,
        humidity = mergedHumidity,
        apparentTemperature = mergedApparentTemperature,
        heatLevel = mergedHeatLevel,
        skyStatus = mergedSkyStatus,
        temperatureDelta = mergedTemperatureDelta,
        coldWaveWarning = mergedColdWaveWarning,
        observedAt = latestWeatherObservedAt,
        weatherFieldProvenance =
            WeatherFieldProvenance(
                observedAtByField = selectedTimestamps,
                fieldsFromPreviousValue = fieldsFromPrevious,
            ),
    )
}

private data class SelectedWeatherField<T>(
    val value: T?,
    val timestamp: String?,
    val fromPreviousValue: Boolean,
)

private fun <T> selectWeatherField(
    incomingValue: T?,
    incomingTimestamp: String?,
    previousValue: T?,
    previousTimestamp: String?,
    now: Instant,
): SelectedWeatherField<T> {
    val freshIncomingTimestamp = incomingTimestamp?.takeIf { it.isFreshWeatherTimestamp(now) }
    val freshPreviousTimestamp = previousTimestamp?.takeIf { it.isFreshWeatherTimestamp(now) }
    val incomingIsOlder =
        freshIncomingTimestamp != null && freshPreviousTimestamp != null &&
            (freshIncomingTimestamp.toWeatherInstantOrNull() ?: Instant.MIN)
                .isBefore(freshPreviousTimestamp.toWeatherInstantOrNull() ?: Instant.MIN)
    if (incomingValue != null && freshIncomingTimestamp != null && !incomingIsOlder) {
        return SelectedWeatherField(incomingValue, freshIncomingTimestamp, fromPreviousValue = false)
    }
    if (previousValue != null && freshPreviousTimestamp != null) {
        return SelectedWeatherField(previousValue, freshPreviousTimestamp, fromPreviousValue = true)
    }
    return SelectedWeatherField(null, null, fromPreviousValue = false)
}

private fun Double.toSignedDisplayNumber(): String =
    if (this > 0.0) {
        "+".plus(toDisplayNumber())
    } else {
        toDisplayNumber()
    }

private fun Double.toIncreasingDirection(): Boolean? =
    when {
        this > 0.0 -> true
        this < 0.0 -> false
        else -> null
    }

private fun ColdWaveWarning?.confirmedForHome(): ColdWaveWarning? =
    this
        ?.takeIf { warning ->
            warning.availability == ColdWaveAvailability.AVAILABLE ||
                warning.availability == ColdWaveAvailability.CACHED
        }?.takeIf { warning ->
            warning.level == ColdWaveLevel.NONE ||
                warning.level == ColdWaveLevel.ADVISORY ||
                warning.level == ColdWaveLevel.WARNING
        }
