package com.nativelap.heartguard.view.route.record

import android.view.WindowManager
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.view.component.RecordType
import com.nativelap.heartguard.view.component.record.RecordCancelConfirmDialogContent
import com.nativelap.heartguard.viewmodel.record.RecordDraftViewModel
import com.nativelap.heartguard.viewmodel.record.RecordSubmissionState

@Composable
internal fun RecordCancelConfirmRoute(
    recordDraftViewModel: RecordDraftViewModel,
    onContinueClick: () -> Unit,
    onGoHomeClick: () -> Unit,
) {
    val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window

    DisposableEffect(dialogWindow) {
        if (dialogWindow == null) {
            onDispose {}
        } else {
            val originalDimAmount = dialogWindow.attributes.dimAmount
            val originallyDimmedBehind = dialogWindow.attributes.flags and
                WindowManager.LayoutParams.FLAG_DIM_BEHIND != 0
            dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            dialogWindow.setDimAmount(RECORD_CANCEL_DIALOG_DIM_AMOUNT)

            onDispose {
                dialogWindow.setDimAmount(originalDimAmount)
                if (originallyDimmedBehind) {
                    dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                } else {
                    dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                }
            }
        }
    }

    val draftState by recordDraftViewModel.uiState.collectAsStateWithLifecycle()
    val submissionState by recordDraftViewModel.submissionState.collectAsStateWithLifecycle()
    val canLeaveRecordFlow =
        submissionState is RecordSubmissionState.Idle ||
            submissionState is RecordSubmissionState.Failure
    val isSaving = !canLeaveRecordFlow
    val titleResource = when (draftState.selectedRecordType) {
        RecordType.TEMPERATURE -> R.string.record_cancel_confirm_title
        RecordType.WORK -> R.string.record_cancel_confirm_work_title
        RecordType.REST -> R.string.record_cancel_confirm_rest_title
        null -> R.string.record_cancel_confirm_generic_title
    }
    val messageResource = when {
        isSaving -> R.string.record_cancel_confirm_saving_message
        draftState.selectedRecordType == RecordType.TEMPERATURE -> {
            R.string.record_cancel_confirm_message
        }

        else -> R.string.record_cancel_confirm_generic_message
    }
    val message = stringResource(
        messageResource,
    )

    RecordCancelConfirmDialogContent(
        title = stringResource(titleResource),
        message = message,
        continueTitle = stringResource(R.string.record_cancel_confirm_continue),
        goHomeTitle = stringResource(R.string.record_cancel_confirm_home),
        isSaving = isSaving,
        modifier = Modifier.padding(horizontal = HeartGuardSpacing.Section),
        onContinueClick = onContinueClick,
        onGoHomeClick = {
            if (canLeaveRecordFlow) {
                onGoHomeClick()
            }
        },
    )
}

private const val RECORD_CANCEL_DIALOG_DIM_AMOUNT = 0.4f
