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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nativelap.heartguard.R
import com.nativelap.heartguard.core.component.overlay.ApplyDialogWindowBackgroundBlur
import com.nativelap.heartguard.core.util.isValidPhoneNumber
import com.nativelap.heartguard.core.util.rememberPhoneDialLauncher
import com.nativelap.heartguard.ui.theme.HeartGuardOverlayBlur
import com.nativelap.heartguard.view.component.emergency.formatKoreanPhoneNumberForDisplay
import com.nativelap.heartguard.view.component.home.HomeEmergencyPhoneCallDialog
import com.nativelap.heartguard.view.component.valueOrEmptyText
import com.nativelap.heartguard.viewmodel.home.HomeViewModel

@Composable
internal fun HomeHeadquartersCallConfirmationRoute(
    homeViewModel: HomeViewModel,
    onDismissRequest: () -> Unit,
    onDialerOpened: () -> Unit,
) {
    val context = LocalContext.current
    val siteStatus by homeViewModel.siteStatus.collectAsStateWithLifecycle()
    val managerPhoneNumber = siteStatus.managerPhoneNumber
    val validManagerPhoneNumber = managerPhoneNumber?.takeIf(String::isValidPhoneNumber)
    val callFailedMessage = stringResource(R.string.home_call_failed)
    val dialPhoneNumber = rememberPhoneDialLauncher()
    var isCallActionInProgress by rememberSaveable {
        mutableStateOf(false)
    }
    val currentOnDialerOpened by rememberUpdatedState(onDialerOpened)

    val openManagerDialer = {
        if (!isCallActionInProgress && validManagerPhoneNumber != null) {
            isCallActionInProgress = true
            if (dialPhoneNumber(validManagerPhoneNumber)) {
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
        phoneNumber = valueOrEmptyText(validManagerPhoneNumber?.let(::formatKoreanPhoneNumberForDisplay)),
        isCallEnabled = validManagerPhoneNumber != null,
        onDismissRequest = onDismissRequest,
        onCallClick = openManagerDialer,
    )
}
