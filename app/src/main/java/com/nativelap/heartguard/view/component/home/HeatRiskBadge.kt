package com.nativelap.heartguard.view.component.home

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme

// TODO: Figma 02_홈_리디자인 실측값(배경 #FFE2D6, 텍스트 #FF6B00)과 일치하는
//  warningContainer/onWarningContainer 디자인 토큰이 Theme.kt에 추가되면 이 임시 상수를 제거한다.
private val HeatRiskBadgeContainerColor = Color(0xFFFFE2D6)
private val HeatRiskBadgeContentColor = Color(0xFFFF6B00)

/** 폭염 위험도를 짧은 상태 배지로 보여준다. */
@Composable
fun HeatRiskBadge(
    riskLabel: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(HeartGuardRadius.Pill),
        color = HeatRiskBadgeContainerColor,
        contentColor = HeatRiskBadgeContentColor,
    ) {
        Text(
            text = riskLabel,
            modifier = Modifier.padding(
                horizontal = HeartGuardSpacing.Item,
                vertical = HeartGuardSpacing.Tight,
            ),
            style = MaterialTheme.typography.labelMedium.copy(
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
