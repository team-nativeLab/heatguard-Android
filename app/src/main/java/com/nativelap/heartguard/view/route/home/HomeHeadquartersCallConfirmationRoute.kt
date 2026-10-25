package com.nativelap.heartguard.view.route.home

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.core.component.overlay.ApplyDialogWindowBackgroundBlur
import com.nativelap.heartguard.core.util.rememberPhoneDialLauncher
import com.nativelap.heartguard.ui.theme.HeartGuardOverlayBlur
import com.nativelap.heartguard.view.component.home.HomeEmergencyPhoneCallDialog

@Composable
internal fun HomeHeadquartersCallConfirmationRoute(
    onDismissRequest: () -> Unit,
    onDialerOpened: () -> Unit,
) {
    val context = LocalContext.current
    val headquartersPhoneNumber = stringResource(R.string.home_emergency_phone_number)
    val callFailedMessage = stringResource(R.string.home_call_failed)
    val dialPhoneNumber = rememberPhoneDialLauncher()
    var isCallActionInProgress by rememberSaveable {
        mutableStateOf(false)
    }
    val currentOnDialerOpened by rememberUpdatedState(onDialerOpened)

    val openHeadquartersDialer = {
        if (!isCallActionInProgress) {
            isCallActionInProgress = true
            if (dialPhoneNumber(headquartersPhoneNumber)) {
                currentOnDialerOpened()
            } else {
                isCallActionInProgress = false
                Toast.makeText(context, callFailedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    ApplyDialogWindowBackgroundBlur(
        blurRadius = HeartGuardOverlayBlur.FigmaBackdrop,
    )

    HomeEmergencyPhoneCallDialog(
        phoneNumber = headquartersPhoneNumber,
        onDismissRequest = onDismissRequest,
        onCallClick = openHeadquartersDialer,
    )
}
