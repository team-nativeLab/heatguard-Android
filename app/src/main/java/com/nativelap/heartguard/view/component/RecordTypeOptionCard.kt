package com.nativelap.heartguard.view.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardExtraColors
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 아이콘, 제목, 설명을 하나의 접근 가능한 단일 선택 카드로 표현한다. */
@Composable
fun RecordTypeOptionCard(
    title: String,
    description: String,
    iconPainter: Painter,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extraColors: HeartGuardExtraColors = MaterialTheme.extraColors

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HeartGuardComponentSize.RecordOptionMinHeight)
            .clip(RoundedCornerShape(HeartGuardRadius.Card))
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(HeartGuardRadius.Card),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(HeartGuardSpacing.Hairline, extraColors.cardBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = HeartGuardSpacing.Section,
                    vertical = HeartGuardSpacing.Section,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
        ) {
            Image(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.size(HeartGuardIconSize.RecordOption),
            )
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = HeartGuardFontSize.RecordOptionTitle,
                        lineHeight = HeartGuardFontSize.RecordOptionTitle,
                    ),
                )
                Spacer(modifier = Modifier.height(HeartGuardSpacing.Compact))
                Text(
                    text = description,
                    color = extraColors.mutedText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = HeartGuardFontSize.RecordOptionDescription,
                        lineHeight = HeartGuardFontSize.RecordOptionDescription,
                    ),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun RecordTypeOptionCardPreview() {
    HeartGuardTheme {
        RecordTypeOptionCard(
            title = stringResource(R.string.record_work_photo_title),
            description = stringResource(R.string.record_work_photo_description),
            iconPainter = painterResource(R.drawable.record_work_photo),
            isSelected = false,
            onClick = {},
        )
    }
}
