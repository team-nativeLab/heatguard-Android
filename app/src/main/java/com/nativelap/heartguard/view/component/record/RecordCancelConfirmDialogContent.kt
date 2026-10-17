package com.nativelap.heartguard.view.component.record

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.extraColors

@Composable
internal fun RecordCancelConfirmDialogContent(
    title: String,
    message: String,
    continueTitle: String,
    goHomeTitle: String,
    isSaving: Boolean,
    onContinueClick: () -> Unit,
    onGoHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .widthIn(max = HeartGuardComponentSize.DialogMaxWidth)
            .fillMaxWidth()
            .heightIn(min = HeartGuardComponentSize.RecordCancelDialogMinHeight),
        shape = RoundedCornerShape(HeartGuardRadius.Dialog),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(
                start = HeartGuardSpacing.Section,
                top = HeartGuardSpacing.DialogTop,
                end = HeartGuardSpacing.Section,
                bottom = HeartGuardSpacing.DialogBottom,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
        ) {
            Box(
                modifier = Modifier
                    .size(HeartGuardIconSize.DialogIcon)
                    .background(
                        color = MaterialTheme.extraColors.dangerContainer,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PriorityHigh,
                    contentDescription = null,
                    modifier = Modifier.size(HeartGuardIconSize.DialogGlyph),
                    tint = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(HeartGuardSpacing.Tight))

            Text(
                text = title,
                color = MaterialTheme.extraColors.strongText,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = HeartGuardFontSize.DialogTitle,
                    lineHeight = HeartGuardFontSize.DialogTitleLineHeight,
                    fontWeight = FontWeight.Bold,
                ),
                textAlign = TextAlign.Center,
            )

            Text(
                text = message,
                color = MaterialTheme.extraColors.secondaryText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = HeartGuardFontSize.DialogBody,
                    lineHeight = HeartGuardFontSize.BodyLineHeight,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(HeartGuardSpacing.DialogActionGap))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
            ) {
                Button(
                    onClick = onContinueClick,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = HeartGuardComponentSize.PrimaryButtonHeight),
                    shape = RoundedCornerShape(HeartGuardRadius.PrimaryAction),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.extraColors.strongText,
                    ),
                ) {
                    Text(
                        text = continueTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }

                Button(
                    onClick = onGoHomeClick,
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = HeartGuardComponentSize.PrimaryButtonHeight),
                    shape = RoundedCornerShape(HeartGuardRadius.PrimaryAction),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(
                        text = goHomeTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }
    }
}
