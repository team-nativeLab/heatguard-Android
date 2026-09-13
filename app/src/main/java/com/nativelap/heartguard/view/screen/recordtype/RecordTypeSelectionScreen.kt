package com.nativelap.heartguard.view.screen.recordtype

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.RecordTypeOptionUiModel
import com.nativelap.heartguard.view.component.RecordTypeSelectionSheet

/** 홈 위에 기록 유형 선택 Sheet를 정적인 디자인 상태로 조합한다. */
@Composable
fun RecordTypeSelectionScreen(
    selectedKey: String?,
    onOptionSelected: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            RecordTypeScrim()

            RecordTypeSelectionSheet(
                title = stringResource(R.string.record_type_selection_title),
                options = recordTypeOptions(),
                selectedKey = selectedKey,
                onOptionSelected = onOptionSelected,
                confirmTitle = stringResource(R.string.common_confirm),
                onConfirm = onConfirm,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxHeight(RECORD_TYPE_SHEET_HEIGHT_FRACTION),
            )
        }
    }
}

/** 선택 Sheet 뒤의 콘텐츠를 분리해 전경 카드가 명확하게 보이도록 한다. */
@Composable
private fun BoxScope.RecordTypeScrim() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = RECORD_TYPE_SCRIM_ALPHA)),
    )
}

@Composable
private fun recordTypeOptions(): List<RecordTypeOptionUiModel> {
    return listOf(
        RecordTypeOptionUiModel(
            key = "temperature",
            title = stringResource(R.string.record_temperature_title),
            description = stringResource(R.string.record_temperature_description),
            iconPainter = painterResource(R.drawable.record_temperature),
        ),
        RecordTypeOptionUiModel(
            key = "work",
            title = stringResource(R.string.record_work_photo_title),
            description = stringResource(R.string.record_work_photo_description),
            iconPainter = painterResource(R.drawable.record_work_photo),
        ),
        RecordTypeOptionUiModel(
            key = "rest",
            title = stringResource(R.string.record_rest_photo_title),
            description = stringResource(R.string.record_rest_photo_description),
            iconPainter = painterResource(R.drawable.record_rest_photo),
        ),
    )
}

private const val RECORD_TYPE_SCRIM_ALPHA = 0.32f
private const val RECORD_TYPE_SHEET_HEIGHT_FRACTION = 0.73f

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun RecordTypeSelectionScreenDefaultPreview() {
    HeartGuardTheme {
        RecordTypeSelectionScreen(
            selectedKey = null,
            onOptionSelected = {},
            onConfirm = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun RecordTypeSelectionScreenSelectedPreview() {
    HeartGuardTheme {
        RecordTypeSelectionScreen(
            selectedKey = "temperature",
            onOptionSelected = {},
            onConfirm = {},
        )
    }
}
