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

/** 폭염 위험도를 짧은 상태 배지로 보여준다. */
@Composable
fun HeatRiskBadge(
    riskLabel: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(HeartGuardRadius.Pill),
        color = MaterialTheme.extraColors.warningContainer,
        contentColor = MaterialTheme.extraColors.onWarningContainer,
    ) {
        Text(
            text = riskLabel,
            modifier = Modifier.padding(
                horizontal = HeartGuardSpacing.BadgeHorizontal,
                vertical = HeartGuardSpacing.BadgeVertical,
            ),
            style = MaterialTheme.typography.labelSmall.copy(
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
