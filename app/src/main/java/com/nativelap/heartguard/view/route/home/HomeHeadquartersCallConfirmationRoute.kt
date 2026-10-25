package com.nativelap.heartguard.view.route.home

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.nativelap.heartguard.R
import com.nativelap.heartguard.core.component.overlay.ApplyDialogWindowBackgroundBlur
import com.nativelap.heartguard.core.util.rememberPhoneCallLauncher
import com.nativelap.heartguard.ui.theme.HeartGuardOverlayBlur
import com.nativelap.heartguard.view.component.home.HomeEmergencyPhoneCallDialog

@Composable
internal fun HomeHeadquartersCallConfirmationRoute(
    onDismissRequest: () -> Unit,
    onPermissionDenied: () -> Unit,
    onCallStarted: () -> Unit,
) {
    val context = LocalContext.current
    val headquartersPhoneNumber = stringResource(R.string.home_emergency_phone_number)
    val callFailedMessage = stringResource(R.string.home_call_failed)
    val callPhoneNumber = rememberPhoneCallLauncher()
    var isPermissionRequestInProgress by rememberSaveable {
        mutableStateOf(false)
    }
    var isCallActionInProgress by rememberSaveable {
        mutableStateOf(false)
    }
    val currentOnCallStarted by rememberUpdatedState(onCallStarted)
    val currentOnPermissionDenied by rememberUpdatedState(onPermissionDenied)

    val startHeadquartersCall = {
        if (!isCallActionInProgress) {
            isCallActionInProgress = true
            if (callPhoneNumber(headquartersPhoneNumber)) {
                currentOnCallStarted()
            } else {
                isCallActionInProgress = false
                Toast.makeText(context, callFailedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val callPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isPermissionGranted ->
            isPermissionRequestInProgress = false
            if (isPermissionGranted) {
                startHeadquartersCall()
            } else {
                currentOnPermissionDenied()
            }
        }

    ApplyDialogWindowBackgroundBlur(
        blurRadius = HeartGuardOverlayBlur.FigmaBackdrop,
    )

    HomeEmergencyPhoneCallDialog(
        phoneNumber = headquartersPhoneNumber,
        onDismissRequest = onDismissRequest,
        onCallClick = {
            val hasCallPermission =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CALL_PHONE,
                ) == PackageManager.PERMISSION_GRANTED
            if (hasCallPermission) {
                startHeadquartersCall()
            } else if (!isPermissionRequestInProgress) {
                isPermissionRequestInProgress = true
                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            }
        },
    )
}
