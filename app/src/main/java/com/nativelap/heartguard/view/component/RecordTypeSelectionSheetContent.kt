package com.nativelap.heartguard.view.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme

/** 기록 유형 선택 콘텐츠를 조합하며, 실제 BottomSheet 상태와 화면 이동은 소유하지 않는다. */
@Composable
fun RecordTypeSelectionSheetContent(
    options: List<RecordTypeOptionUiModel>,
    onOptionSelected: (Int) -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(
                start = HeartGuardSpacing.RecordContentHorizontal,
                top = HeartGuardSpacing.Section,
                end = HeartGuardSpacing.RecordContentHorizontal,
                bottom = HeartGuardSpacing.Section,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
    ) {
        RecordTypeSelectionTitle(
            title = stringResource(R.string.record_type_selection_title),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(HeartGuardSpacing.Section - HeartGuardSpacing.Compact))
        options.forEachIndexed { optionIndex, option ->
            RecordTypeOptionCard(
                title = option.title,
                description = option.description,
                iconPainter = painterResource(option.iconResId),
                isSelected = option.isSelected,
                onClick = { onOptionSelected(optionIndex) },
            )
        }
        Spacer(modifier = Modifier.height(HeartGuardSpacing.Section - HeartGuardSpacing.Compact))
        RecordTypeConfirmButton(
            title = stringResource(R.string.common_confirm),
            isEnabled = options.any(RecordTypeOptionUiModel::isSelected),
            onClick = onConfirmClick,
        )
    }
}

/** 선택 콘텐츠가 필요한 리소스와 선택 상태만 전달받도록 만든 표시 모델이다. */
@Immutable
data class RecordTypeOptionUiModel(
    @get:DrawableRes val iconResId: Int,
    val title: String,
    val description: String,
    val isSelected: Boolean,
)

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun RecordTypeSelectionSheetContentPreview() {
    HeartGuardTheme {
        RecordTypeSelectionSheetContent(
            options = recordTypePreviewOptions(selectedIndex = 0),
            onOptionSelected = {},
            onConfirmClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun RecordTypeSelectionSheetContentDisabledPreview() {
    HeartGuardTheme {
        RecordTypeSelectionSheetContent(
            options = recordTypePreviewOptions(selectedIndex = null),
            onOptionSelected = {},
            onConfirmClick = {},
        )
    }
}

@Composable
private fun recordTypePreviewOptions(selectedIndex: Int?): List<RecordTypeOptionUiModel> {
    return listOf(
        RecordTypeOptionUiModel(
            iconResId = R.drawable.record_temperature,
            title = stringResource(R.string.record_temperature_title),
            description = stringResource(R.string.record_temperature_description),
            isSelected = selectedIndex == 0,
        ),
        RecordTypeOptionUiModel(
            iconResId = R.drawable.record_work_photo,
            title = stringResource(R.string.record_work_photo_title),
            description = stringResource(R.string.record_work_photo_description),
            isSelected = selectedIndex == 1,
        ),
        RecordTypeOptionUiModel(
            iconResId = R.drawable.record_rest_photo,
            title = stringResource(R.string.record_rest_photo_title),
            description = stringResource(R.string.record_rest_photo_description),
            isSelected = selectedIndex == 2,
        ),
    )
}
