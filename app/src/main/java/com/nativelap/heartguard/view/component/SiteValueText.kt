package com.nativelap.heartguard.view.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R

// 서버에서 받은 값을 화면 문자열로 바꾸는 공용 규칙이다. 값이 null이면(응답 전·실패·API 없음) 항상 "--"를 돌려준다.

@Composable
@ReadOnlyComposable
fun emptyValueText(): String = stringResource(R.string.common_empty_value)

@Composable
@ReadOnlyComposable
fun valueOrEmptyText(value: String?): String = value ?: emptyValueText()

@Composable
@ReadOnlyComposable
fun temperatureValueText(temperature: String?): String =
    if (temperature == null) {
        emptyValueText()
    } else {
        stringResource(R.string.common_temperature_value_format, temperature)
    }

@Composable
@ReadOnlyComposable
fun humidityValueText(humidity: String?): String =
    if (humidity == null) {
        emptyValueText()
    } else {
        stringResource(R.string.home_humidity_value_format, humidity)
    }

/** 폭염 단계(0 주의보 없음·1 주의·2 경고·3 위험) 라벨이다. 값이 없거나 범위를 벗어나면 "관측값 없음"이다. */
@Composable
@ReadOnlyComposable
fun heatLevelLabelText(heatLevel: Int?): String =
    when (heatLevel) {
        0 -> stringResource(R.string.home_heat_level_interest)
        1 -> stringResource(R.string.home_heat_caution)
        2 -> stringResource(R.string.home_heat_level_warning)
        3 -> stringResource(R.string.home_heat_level_danger)
        else -> stringResource(R.string.home_heat_level_unknown)
    }
