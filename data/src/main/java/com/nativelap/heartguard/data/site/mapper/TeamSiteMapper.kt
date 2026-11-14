package com.nativelap.heartguard.data.site.mapper

import com.nativelap.heartguard.data.site.dto.TeamSiteResponseDto
import com.nativelap.heartguard.domain.site.model.SkyStatus
import com.nativelap.heartguard.domain.site.model.TeamSiteOverview
import com.nativelap.heartguard.domain.site.model.WeatherField
import com.nativelap.heartguard.domain.site.model.WeatherFieldProvenance

// 서버가 빈 문자열로 준 값(예: 관리자 연락처 미등록)은 "값 없음"과 같게 취급해 화면이 "--"로 표시하게 한다.
internal fun TeamSiteResponseDto.toDomain(): TeamSiteOverview =
    TeamSiteOverview(
        teamName = team.name.nullIfBlank(),
        workplace = team.workplace.nullIfBlank(),
        siteName = site.name.nullIfBlank(),
        managerPhoneNumber = site.managerPhone.nullIfBlank(),
        currentTemperature = weather?.temperature,
        humidity = weather?.humidity,
        apparentTemperature = weather?.apparentTemperature,
        heatLevel = heatLevel,
        checkTimes = checkTimes,
        hasActiveEmergencyCall = activeEmergencyCall?.callId != null,
        headquartersPhoneNumber = company?.phone.nullIfBlank(),
        skyStatus = weather?.skyStatus.nullIfBlank()?.toSkyStatus(),
        temperatureDelta = weather?.temperatureDelta,
        coldWaveWarning = weather?.coldWaveWarning?.toDomain(),
        observedAt = weather?.observedAt,
        weatherFieldProvenance =
            WeatherFieldProvenance(
                observedAtByField =
                    buildMap {
                        if (weather?.temperature != null && weather.observedAt != null) {
                            put(WeatherField.TEMPERATURE, weather.observedAt)
                        }
                        if (weather?.humidity != null && weather.observedAt != null) {
                            put(WeatherField.HUMIDITY, weather.observedAt)
                        }
                        if (weather?.apparentTemperature != null && weather.observedAt != null) {
                            put(WeatherField.APPARENT_TEMPERATURE, weather.observedAt)
                        }
                        if (heatLevel != null && weather?.observedAt != null) {
                            put(WeatherField.HEAT_LEVEL, weather.observedAt)
                        }
                        if (weather?.skyStatus != null && weather.observedAt != null) {
                            put(WeatherField.SKY_STATUS, weather.observedAt)
                        }
                        if (weather?.temperatureDelta != null && weather.observedAt != null) {
                            put(WeatherField.TEMPERATURE_DELTA, weather.observedAt)
                        }
                        weather?.coldWaveWarning?.fetchedAt?.let { fetchedAt ->
                            put(WeatherField.COLD_WAVE_WARNING, fetchedAt)
                        }
                    },
            ),
    )

private fun String.toSkyStatus(): SkyStatus =
    when (this) {
        "CLEAR" -> SkyStatus.CLEAR
        "PARTLY_CLOUDY" -> SkyStatus.PARTLY_CLOUDY
        "CLOUDY" -> SkyStatus.CLOUDY
        "RAIN" -> SkyStatus.RAIN
        "SNOW" -> SkyStatus.SNOW
        else -> SkyStatus.UNKNOWN
    }

private fun String?.nullIfBlank(): String? = this?.takeIf { text -> text.isNotBlank() }
