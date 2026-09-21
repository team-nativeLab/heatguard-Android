package com.nativelap.heartguard.view.component.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
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

/** 현장 사진·기록 내역처럼 추가 기록으로 이동하는 한 줄 카드를 표현한다. */
@Composable
fun HomeAdditionalRecordCard(
    title: String,
    description: String,
    iconPainter: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HeartGuardComponentSize.HomeAdditionalRecordHeight)
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(HeartGuardRadius.HomeAction),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HeartGuardSpacing.Section),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
        ) {
            Surface(
                modifier = Modifier.size(HeartGuardIconSize.HomeRecord),
                shape = RoundedCornerShape(HeartGuardRadius.HomeAction),
                color = MaterialTheme.extraColors.homeMetricContainer,
            ) {
                Image(
                    painter = iconPainter,
                    contentDescription = null,
                    modifier = Modifier.padding(HeartGuardSpacing.Item),
                )
            }
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
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
                text = "›",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun HomeAdditionalRecordCardPreview() {
    HeartGuardTheme {
        HomeAdditionalRecordCard(
            title = "현장 사진",
            description = "사진 촬영 또는 앨범에서 선택",
            iconPainter = androidx.compose.ui.res.painterResource(R.drawable.home_photo),
            onClick = {},
        )
    }
}
