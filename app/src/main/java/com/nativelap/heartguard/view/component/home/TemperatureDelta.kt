package com.nativelap.heartguard.view.component.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 현재 온도와 비교한 변화량을 방향 화살표가 붙은 Figma 강조 pill과 접근성 설명으로 표현한다.
 * [isIncreasing]이 null이면(변화량을 서버에서 받지 못함) 화살표 없이 중립 색으로 [deltaValue]만 보여준다. */
@Composable
fun TemperatureDelta(
    deltaLabel: String,
    deltaValue: String,
    isIncreasing: Boolean?,
    isTemperatureDeltaZero: Boolean,
    modifier: Modifier = Modifier,
) {
    val directionalDeltaText =
        when (isIncreasing) {
            true -> stringResource(R.string.home_temperature_rise_format, deltaValue)
            false -> stringResource(R.string.home_temperature_fall_format, deltaValue)
            null -> deltaValue
        }

    Surface(
        modifier =
            modifier.semantics {
                contentDescription = "$deltaLabel $deltaValue"
            },
        shape = RoundedCornerShape(HeartGuardRadius.Pill),
        color =
            when {
                isIncreasing == true -> {
                    MaterialTheme.extraColors.temperatureRiseContainer
                }

                isIncreasing == false || isTemperatureDeltaZero -> {
                    MaterialTheme.extraColors.temperatureDeltaNonPositiveContainer
                }

                else -> {
                    MaterialTheme.extraColors.homeMetricContainer
                }
            },
        contentColor =
            when {
                isIncreasing == true -> {
                    MaterialTheme.extraColors.onTemperatureRiseContainer
                }

                isIncreasing == false || isTemperatureDeltaZero -> {
                    MaterialTheme.extraColors.onTemperatureDeltaNonPositiveContainer
                }

                else -> {
                    MaterialTheme.extraColors.homeMutedText
                }
            },
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = directionalDeltaText,
                modifier =
                    Modifier.padding(
                        horizontal = HeartGuardSpacing.Compact,
                        vertical = HeartGuardSpacing.Tight,
                    ),
                textAlign = TextAlign.Center,
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontSize = HeartGuardFontSize.TemperatureDelta,
                        fontWeight = FontWeight.Bold,
                    ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 240)
@Composable
private fun TemperatureDeltaPreview() {
    HeartGuardTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
        ) {
            TemperatureDelta(
                deltaLabel = "온도 변화",
                deltaValue = "-3.2°C",
                isIncreasing = false,
                isTemperatureDeltaZero = false,
            )
            TemperatureDelta(
                deltaLabel = "온도 변화",
                deltaValue = "0°C",
                isIncreasing = null,
                isTemperatureDeltaZero = true,
            )
            TemperatureDelta(
                deltaLabel = "온도 변화",
                deltaValue = "--",
                isIncreasing = null,
                isTemperatureDeltaZero = false,
            )
            TemperatureDelta(
                deltaLabel = "온도 변화",
                deltaValue = "+3.2°C",
                isIncreasing = true,
                isTemperatureDeltaZero = false,
            )
        }
    }
}
