package com.nativelap.heartguard.view.component.home

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.nativelap.heartguard.R

@Composable
fun HomeEmergencyPhoneCallDialog(
    phoneNumber: String,
    onDismissRequest: () -> Unit,
    onCallClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onCallClick) {
                Text(text = stringResource(R.string.home_emergency_call_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.home_emergency_call_dismiss))
            }
        },
        text = {
            Text(
                text = phoneNumber,
                textAlign = TextAlign.Center,
            )
        },
        modifier = modifier,
    )
}
