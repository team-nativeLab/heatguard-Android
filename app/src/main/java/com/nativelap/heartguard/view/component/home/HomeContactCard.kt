package com.nativelap.heartguard.view.component.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 관리자 전화와 긴급 전화를 Figma의 두 행 연락처 카드로 제공한다. */
@Composable
fun HomeContactCard(
    managerTitle: String,
    managerDescription: String,
    emergencyTitle: String,
    emergencyDescription: String,
    onManagerClick: () -> Unit,
    onEmergencyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.HomeAction),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            HomeContactRow(
                title = managerTitle,
                description = managerDescription,
                iconPainter = painterResource(R.drawable.home_manager_phone),
                iconContainerColor = MaterialTheme.extraColors.homeMetricContainer,
                onClick = onManagerClick,
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = HeartGuardSpacing.Section),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            HomeContactRow(
                title = emergencyTitle,
                description = emergencyDescription,
                iconPainter = painterResource(R.drawable.home_emergency_phone),
                iconContainerColor = MaterialTheme.extraColors.homeContactAlertContainer,
                onClick = onEmergencyClick,
            )
        }
    }
}

/** 연락처 카드의 한 행을 터치 가능한 65dp 영역으로 표현한다. */
@Composable
private fun HomeContactRow(
    title: String,
    description: String,
    iconPainter: Painter,
    iconContainerColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = HeartGuardComponentSize.HomeContactHeight / 2)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = HeartGuardSpacing.Section),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
    ) {
        Surface(
            modifier = Modifier.size(HeartGuardIconSize.HomeRecord),
            shape = RoundedCornerShape(HeartGuardRadius.HomeAction),
            color = iconContainerColor,
        ) {
            Image(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.padding(HeartGuardSpacing.Item),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = description,
                color = MaterialTheme.extraColors.homeMutedText,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = stringResource(R.string.common_chevron_right),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun HomeContactCardPreview() {
    HeartGuardTheme {
        HomeContactCard(
            managerTitle = "관리자 전화",
            managerDescription = "현장 관리자에게 연락",
            emergencyTitle = "긴급 전화",
            emergencyDescription = "본사와 즉시 연결",
            onManagerClick = {},
            onEmergencyClick = {},
        )
    }
}
