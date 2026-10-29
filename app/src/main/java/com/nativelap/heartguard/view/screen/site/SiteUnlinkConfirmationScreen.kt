package com.nativelap.heartguard.view.screen.site

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing

@Composable
internal fun SiteUnlinkConfirmationScreen(
    onCancelClick: () -> Unit,
    onUnlinkClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .widthIn(max = HeartGuardComponentSize.DialogMaxWidth)
                .fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.Dialog),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(HeartGuardSpacing.Section),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(SITE_UNLINK_ICON_CONTAINER_SIZE)
                        .clip(RoundedCornerShape(HeartGuardRadius.Pill))
                        .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PriorityHigh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(SITE_UNLINK_ICON_SIZE),
                )
            }
            Text(
                text = stringResource(R.string.app_account_site_unlink_dialog_title),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.app_account_site_unlink_dialog_message),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = HeartGuardSpacing.Tight),
                horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onCancelClick,
                    shape = RoundedCornerShape(HeartGuardRadius.Button),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                ) {
                    Text(text = stringResource(R.string.app_account_site_unlink_cancel))
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onUnlinkClick,
                    shape = RoundedCornerShape(HeartGuardRadius.Button),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                ) {
                    Text(text = stringResource(R.string.app_account_site_unlink_confirm))
                }
            }
        }
    }
}

private val SITE_UNLINK_ICON_CONTAINER_SIZE = 52.dp
private val SITE_UNLINK_ICON_SIZE = 24.dp
