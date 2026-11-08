package com.nativelap.heartguard.view.component.home

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 폭염 위험도는 주황색으로, 홈의 한파 특보는 파란색으로 표시한다. */
@Composable
fun HeatRiskBadge(
    riskLabel: String,
    modifier: Modifier = Modifier,
    isColdWaveAlert: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(HeartGuardRadius.Pill),
        color =
            if (isColdWaveAlert) {
                MaterialTheme.extraColors.coldWaveWarningContainer
            } else {
                MaterialTheme.extraColors.warningContainer
            },
        contentColor =
            if (isColdWaveAlert) {
                MaterialTheme.extraColors.onColdWaveWarningContainer
            } else {
                MaterialTheme.extraColors.onWarningContainer
            },
    ) {
        Text(
            text = riskLabel,
            modifier =
                Modifier.padding(
                    horizontal = HeartGuardSpacing.BadgeHorizontal,
                    vertical = HeartGuardSpacing.BadgeVertical,
                ),
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontSize = HeartGuardFontSize.Badge,
                    fontWeight = FontWeight.Bold,
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HeatRiskBadgePreview() {
    HeartGuardTheme {
        HeatRiskBadge(riskLabel = "주의")
    }
}
