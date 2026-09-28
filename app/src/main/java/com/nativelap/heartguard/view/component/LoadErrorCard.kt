package com.nativelap.heartguard.view.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.feedback.RetryButton

/** 서버 조회 실패를 알리고 다시 조회할 수 있게 한다. 빈 결과와 조회 실패를 구분해 보여주기 위한 공용 카드다. */
@Composable
fun LoadErrorCard(
    title: String,
    description: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.Card),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.extraColors.alertContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(HeartGuardSpacing.Item),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
        ) {
            Text(
                text = title,
                color = MaterialTheme.extraColors.strongText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = description,
                color = MaterialTheme.extraColors.secondaryText,
                style = MaterialTheme.typography.bodySmall,
            )
            RetryButton(
                title = stringResource(R.string.common_retry),
                onClick = onRetryClick,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun LoadErrorCardPreview() {
    HeartGuardTheme {
        LoadErrorCard(
            title = "기록을 불러오지 못했어요",
            description = "네트워크 연결을 확인한 뒤 다시 시도해주세요.",
            onRetryClick = {},
        )
    }
}
