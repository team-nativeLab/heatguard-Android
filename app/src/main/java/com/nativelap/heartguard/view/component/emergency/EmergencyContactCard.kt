package com.nativelap.heartguard.view.component.emergency

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 긴급 화면에서 연결할 관리자 이름과 전화번호를 Figma 03/04 연락 대상 카드로 보여준다.
 * 서버에서 전화번호를 아직 받지 못했으면 [isCallEnabled]를 false로 넘겨 카드 탭으로 전화를 걸 수 없게 한다. */
@Composable
fun EmergencyContactCard(
    contactTitle: String,
    contactName: String,
    phoneNumber: String,
    onCallClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCallEnabled: Boolean = true,
) {
    Card(
        onClick = onCallClick,
        enabled = isCallEnabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HeartGuardComponentSize.EmergencyContactHeight),
        shape = RoundedCornerShape(HeartGuardRadius.LargeCard),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(
            width = HeartGuardSpacing.Hairline,
            color = MaterialTheme.extraColors.cardBorder,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = HeartGuardSpacing.LargeSection,
                    top = HeartGuardSpacing.LargeSection,
                    end = HeartGuardSpacing.LargeSection,
                    bottom = HeartGuardSpacing.Section,
                ),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
        ) {
            Text(
                text = contactTitle,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = HeartGuardFontSize.EmergencyContactTitle,
                    fontWeight = FontWeight.Bold,
                ),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = HeartGuardSpacing.EmergencyContactIndent),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contactName,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                        ),
                    )
                    Text(
                        text = phoneNumber,
                        color = MaterialTheme.extraColors.emergencyContactPhone,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                        ),
                    )
                }

                Image(
                    painter = painterResource(R.drawable.emergency_phone),
                    contentDescription = stringResource(R.string.common_call),
                    modifier = Modifier.size(HeartGuardIconSize.Navigation),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun EmergencyContactCardPreview() {
    HeartGuardTheme {
        EmergencyContactCard(
            contactTitle = "연락 대상",
            contactName = "현장 관리자",
            phoneNumber = "010-1234-5678",
            onCallClick = {},
        )
    }
}
