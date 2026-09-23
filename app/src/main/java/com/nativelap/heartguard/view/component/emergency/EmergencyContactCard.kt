package com.nativelap.heartguard.view.component.emergency

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 긴급 화면에서 연결할 관리자 이름과 전화번호를 표시한다. */
@Composable
fun EmergencyContactCard(
    contactTitle: String,
    contactName: String,
    phoneNumber: String,
    onCallClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onCallClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HeartGuardComponentSize.EmergencyContactHeight),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(HeartGuardRadius.LargeCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = HeartGuardSpacing.Hairline,
            color = MaterialTheme.extraColors.cardBorder,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HeartGuardSpacing.Section),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contactTitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = contactName,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = phoneNumber,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Image(
                painter = androidx.compose.ui.res.painterResource(R.drawable.emergency_phone),
                contentDescription = stringResource(R.string.common_call),
                modifier = Modifier.size(HeartGuardIconSize.Information),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun EmergencyContactCardPreview() {
    HeartGuardTheme {
        EmergencyContactCard(
            contactTitle = "현장 관리자",
            contactName = "홍길동",
            phoneNumber = "010-1234-5678",
            onCallClick = {},
        )
    }
}
