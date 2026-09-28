package com.nativelap.heartguard.view.component.profile

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

/** 작업자 정보 조회 실패를 알리고 다시 조회할 수 있게 한다. 빈 값과 조회 실패를 구분하기 위한 안내다. */
@Composable
fun ProfileLoadErrorCard(
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
                text = stringResource(R.string.profile_load_failure_title),
                color = MaterialTheme.extraColors.strongText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = stringResource(R.string.profile_load_failure_description),
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
private fun ProfileLoadErrorCardPreview() {
    HeartGuardTheme {
        ProfileLoadErrorCard(onRetryClick = {})
    }
}
