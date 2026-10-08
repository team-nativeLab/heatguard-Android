package com.nativelap.heartguard.view.component.home

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.extraColors

/** 현재 온도와 비교한 변화량을 방향 화살표가 붙은 Figma 강조 pill과 접근성 설명으로 표현한다.
 * [isIncreasing]이 null이면(변화량을 서버에서 받지 못함) 화살표 없이 중립 색으로 [deltaValue]만 보여준다. */
@Composable
fun TemperatureDelta(
    deltaLabel: String,
    deltaValue: String,
    isIncreasing: Boolean?,
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
            modifier
                .widthIn(min = HeartGuardComponentSize.TemperatureDeltaMinWidth)
                .semantics {
                    contentDescription = "$deltaLabel $deltaValue"
                },
        shape = RoundedCornerShape(HeartGuardRadius.Pill),
        color =
            when (isIncreasing) {
                true -> MaterialTheme.extraColors.temperatureRiseContainer
                false -> MaterialTheme.extraColors.successContainer
                null -> MaterialTheme.extraColors.homeMetricContainer
            },
        contentColor =
            when (isIncreasing) {
                true -> MaterialTheme.extraColors.onTemperatureRiseContainer
                false -> MaterialTheme.extraColors.success
                null -> MaterialTheme.extraColors.homeMutedText
            },
    ) {
        Text(
            text = directionalDeltaText,
            modifier =
                Modifier.padding(
                    horizontal = HeartGuardSpacing.Compact,
                    vertical = HeartGuardSpacing.Tight,
                ),
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
