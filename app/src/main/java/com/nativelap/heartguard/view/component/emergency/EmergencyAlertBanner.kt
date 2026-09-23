package com.nativelap.heartguard.view.component.emergency

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 긴급 화면 상단의 위험 안내 문구를 오류 컨테이너로 강조한다. */
@Composable
fun EmergencyAlertBanner(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(HeartGuardRadius.LargeCard),
        color = MaterialTheme.extraColors.alertContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = HeartGuardComponentSize.EmergencyAlertHeight)
                .padding(horizontal = HeartGuardSpacing.EmergencyAlertHorizontal),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = description,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun EmergencyAlertBannerPreview() {
    HeartGuardTheme {
        EmergencyAlertBanner(
            title = "긴급상황입니다",
            description = "주변 관리자에게 즉시 연락해 주세요.",
        )
    }
}
