package com.nativelap.heartguard.viewmodel.home

import androidx.compose.runtime.Immutable
import com.nativelap.heartguard.domain.site.model.ColdWaveAvailability
import com.nativelap.heartguard.domain.site.model.ColdWaveLevel
import com.nativelap.heartguard.domain.site.model.ColdWaveWarning
import com.nativelap.heartguard.domain.site.model.CurrentLocationWeather
import com.nativelap.heartguard.domain.site.model.SkyStatus
import com.nativelap.heartguard.domain.site.model.TeamSiteOverview
import com.nativelap.heartguard.domain.site.model.WeatherAvailability
import com.nativelap.heartguard.viewmodel.toDisplayNumber

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
fun HomeUiState.toSiteStatusUiModel(locationWeather: CurrentLocationWeather? = null): SiteStatusUiModel {
    val siteStatus =
        (this as? HomeUiState.Success)
            ?.overview
            ?.toSiteStatusUiModel()
            ?: SiteStatusUiModel()
    val availableLocationWeather =
        locationWeather?.takeIf { weather ->
            weather.availability == WeatherAvailability.AVAILABLE ||
                weather.availability == WeatherAvailability.CACHED
        } ?: return siteStatus

    val selectedColdWaveWarning =
        availableLocationWeather.coldWaveWarning.confirmedForHome()
            ?: siteStatus.coldWaveWarning.confirmedForHome()

    return siteStatus.copy(
        temperature = availableLocationWeather.temperature.toDisplayNumber(),
        humidity = availableLocationWeather.humidity.toDisplayNumber(),
        apparentTemperature = availableLocationWeather.apparentTemperature.toDisplayNumber(),
        heatLevel = availableLocationWeather.heatLevel,
        skyStatus = availableLocationWeather.skyStatus,
        temperatureDelta = availableLocationWeather.temperatureDelta?.toSignedDisplayNumber(),
        isTemperatureIncreasing = availableLocationWeather.temperatureDelta?.toIncreasingDirection(),
        isTemperatureDeltaZero = availableLocationWeather.temperatureDelta == 0.0,
        recordTemperature = availableLocationWeather.temperature,
        recordHumidity = availableLocationWeather.humidity,
        coldWaveWarning = selectedColdWaveWarning,
        isColdWaveWarningUnavailable =
            availableLocationWeather.coldWaveWarning?.availability == ColdWaveAvailability.UNAVAILABLE,
    )
}

private fun TeamSiteOverview.toSiteStatusUiModel(): SiteStatusUiModel =
    SiteStatusUiModel(
        temperature = currentTemperature.toDisplayNumber(),
        humidity = humidity.toDisplayNumber(),
        apparentTemperature = apparentTemperature.toDisplayNumber(),
        heatLevel = heatLevel,
        managerPhoneNumber = managerPhoneNumber,
        teamName = teamName,
        workplace = workplace,
        headquartersPhoneNumber = headquartersPhoneNumber,
        skyStatus = skyStatus,
        temperatureDelta = temperatureDelta?.toSignedDisplayNumber(),
        isTemperatureIncreasing = temperatureDelta?.toIncreasingDirection(),
        isTemperatureDeltaZero = temperatureDelta == 0.0,
        recordTemperature = currentTemperature,
        recordHumidity = humidity,
        coldWaveWarning = coldWaveWarning,
    )

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
