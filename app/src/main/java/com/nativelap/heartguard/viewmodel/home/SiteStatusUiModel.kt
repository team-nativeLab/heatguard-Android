package com.nativelap.heartguard.viewmodel.home

import androidx.compose.runtime.Immutable
import com.nativelap.heartguard.domain.site.model.ColdWaveAvailability
import com.nativelap.heartguard.domain.site.model.ColdWaveLevel
import com.nativelap.heartguard.domain.site.model.ColdWaveWarning
import com.nativelap.heartguard.domain.site.model.CurrentLocationWeather
import com.nativelap.heartguard.domain.site.model.SkyStatus
import com.nativelap.heartguard.domain.site.model.TeamSiteOverview
import com.nativelap.heartguard.domain.site.model.WeatherAvailability
import com.nativelap.heartguard.domain.site.model.isFreshWeatherTimestamp
import com.nativelap.heartguard.domain.site.model.elapsedWeatherMinutes
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
): SiteStatusUiModel {
    val siteStatus =
        (this as? HomeUiState.Success)
            ?.overview
            ?.toSiteStatusUiModel(now)
            ?: SiteStatusUiModel()
    val availableLocationWeather =
        locationWeather?.takeIf { weather ->
            weather.availability == WeatherAvailability.AVAILABLE ||
                weather.availability == WeatherAvailability.CACHED
        } ?: return siteStatus
    val isMainWeatherFresh = availableLocationWeather.observedAt.isFreshWeatherTimestamp(now)
    val freshLocationColdWaveWarning =
        availableLocationWeather.coldWaveWarning
            ?.takeIf { warning -> warning.fetchedAt.isFreshWeatherTimestamp(now) }

    val selectedColdWaveWarning =
        freshLocationColdWaveWarning.confirmedForHome()
            ?: siteStatus.coldWaveWarning.confirmedForHome()
    val hasFreshSelectedColdWaveWarning = selectedColdWaveWarning != null

    return siteStatus.copy(
        temperature =
            if (isMainWeatherFresh) {
                availableLocationWeather.temperature.toDisplayNumber()
            } else {
                siteStatus.temperature
            },
        humidity =
            if (isMainWeatherFresh) {
                availableLocationWeather.humidity.toDisplayNumber()
            } else {
                siteStatus.humidity
            },
        apparentTemperature =
            if (isMainWeatherFresh) {
                availableLocationWeather.apparentTemperature.toDisplayNumber()
            } else {
                siteStatus.apparentTemperature
            },
        heatLevel = if (isMainWeatherFresh) availableLocationWeather.heatLevel else siteStatus.heatLevel,
        skyStatus = if (isMainWeatherFresh) availableLocationWeather.skyStatus else siteStatus.skyStatus,
        temperatureDelta =
            if (isMainWeatherFresh) {
                availableLocationWeather.temperatureDelta?.toSignedDisplayNumber()
            } else {
                siteStatus.temperatureDelta
            },
        isTemperatureIncreasing =
            if (isMainWeatherFresh) {
                availableLocationWeather.temperatureDelta?.toIncreasingDirection()
            } else {
                siteStatus.isTemperatureIncreasing
            },
        isTemperatureDeltaZero =
            if (isMainWeatherFresh) {
                availableLocationWeather.temperatureDelta == 0.0
            } else {
                siteStatus.isTemperatureDeltaZero
            },
        recordTemperature =
            if (isMainWeatherFresh) availableLocationWeather.temperature else siteStatus.recordTemperature,
        recordHumidity = if (isMainWeatherFresh) availableLocationWeather.humidity else siteStatus.recordHumidity,
        coldWaveWarning = selectedColdWaveWarning,
        weatherObservedAt =
            if (isMainWeatherFresh) availableLocationWeather.observedAt else siteStatus.weatherObservedAt,
        locationWeatherObservedAt = availableLocationWeather.observedAt.takeIf { isMainWeatherFresh },
        hasLocationWeatherValue = isMainWeatherFresh,
        isLocationWeatherCached =
            isMainWeatherFresh && availableLocationWeather.availability == WeatherAvailability.CACHED,
        coldWaveFetchedAt = selectedColdWaveWarning?.fetchedAt,
        weatherAgeMinutes = availableLocationWeather.observedAt.elapsedWeatherMinutes(now),
        coldWaveAgeMinutes = selectedColdWaveWarning?.fetchedAt.elapsedWeatherMinutes(now),
        isLocationWeatherExpired = !isMainWeatherFresh,
        isColdWaveWarningUnavailable =
            !hasFreshSelectedColdWaveWarning &&
                (availableLocationWeather.coldWaveWarning?.availability == ColdWaveAvailability.UNAVAILABLE ||
                    (availableLocationWeather.coldWaveWarning != null && freshLocationColdWaveWarning == null) ||
                    siteStatus.isColdWaveWarningUnavailable),
    )
}

private fun TeamSiteOverview.toSiteStatusUiModel(now: Instant): SiteStatusUiModel {
    val isMainWeatherFresh = observedAt.isFreshWeatherTimestamp(now)
    val freshColdWaveWarning = coldWaveWarning?.takeIf { warning -> warning.fetchedAt.isFreshWeatherTimestamp(now) }
    return SiteStatusUiModel(
        temperature = currentTemperature.takeIf { isMainWeatherFresh }.toDisplayNumber(),
        humidity = humidity.takeIf { isMainWeatherFresh }.toDisplayNumber(),
        apparentTemperature = apparentTemperature.takeIf { isMainWeatherFresh }.toDisplayNumber(),
        heatLevel = heatLevel.takeIf { isMainWeatherFresh },
        managerPhoneNumber = managerPhoneNumber,
        teamName = teamName,
        workplace = workplace,
        headquartersPhoneNumber = headquartersPhoneNumber,
        skyStatus = skyStatus.takeIf { isMainWeatherFresh },
        temperatureDelta = temperatureDelta.takeIf { isMainWeatherFresh }?.toSignedDisplayNumber(),
        isTemperatureIncreasing = temperatureDelta.takeIf { isMainWeatherFresh }?.toIncreasingDirection(),
        isTemperatureDeltaZero = isMainWeatherFresh && temperatureDelta == 0.0,
        recordTemperature = currentTemperature.takeIf { isMainWeatherFresh },
        recordHumidity = humidity.takeIf { isMainWeatherFresh },
        coldWaveWarning = freshColdWaveWarning,
        isColdWaveWarningUnavailable = coldWaveWarning != null && freshColdWaveWarning == null,
        weatherObservedAt = observedAt.takeIf { isMainWeatherFresh },
        coldWaveFetchedAt = freshColdWaveWarning?.fetchedAt,
        weatherAgeMinutes = observedAt.elapsedWeatherMinutes(now),
        coldWaveAgeMinutes = freshColdWaveWarning?.fetchedAt.elapsedWeatherMinutes(now),
    )
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
