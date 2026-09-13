package com.nativelap.heartguard.view.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

data class RecordTypeOptionUiModel(
    val key: String,
    val title: String,
    val description: String,
    val iconPainter: Painter,
)

/** 기록 유형 선택 화면의 제목·선택 카드·확인 버튼을 바텀시트 형태로 묶는다. */
@Composable
fun RecordTypeSelectionSheet(
    title: String,
    options: List<RecordTypeOptionUiModel>,
    selectedKey: String?,
    onOptionSelected: (String) -> Unit,
    confirmTitle: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = HeartGuardRadius.Sheet,
            topEnd = HeartGuardRadius.Sheet,
        ),
        color = MaterialTheme.extraColors.sheetBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = HeartGuardSpacing.Section,
                    vertical = HeartGuardSpacing.LargeSection,
                ),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
        ) {
            RecordTypeSelectionTitle(
                title = title,
                modifier = Modifier.fillMaxWidth(),
            )
            options.forEach { option ->
                RecordTypeOptionCard(
                    title = option.title,
                    description = option.description,
                    iconPainter = option.iconPainter,
                    isSelected = option.key == selectedKey,
                    onClick = { onOptionSelected(option.key) },
                )
            }
            RecordTypeConfirmButton(
                title = confirmTitle,
                isEnabled = selectedKey != null,
                onClick = onConfirm,
                modifier = Modifier.padding(top = HeartGuardSpacing.Compact),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun RecordTypeSelectionSheetPreview() {
    HeartGuardTheme {
        RecordTypeSelectionSheet(
            title = "기록 유형을 선택하세요",
            options = listOf(
                RecordTypeOptionUiModel(
                    key = "temperature",
                    title = "온도계 사진",
                    description = "온도/습도 입력 후 체감온도 계산",
                    iconPainter = androidx.compose.ui.res.painterResource(R.drawable.record_temperature),
                ),
                RecordTypeOptionUiModel(
                    key = "work",
                    title = "작업 사진",
                    description = "작업 중 사진을 1~2장 업로드",
                    iconPainter = androidx.compose.ui.res.painterResource(R.drawable.record_work_photo),
                ),
                RecordTypeOptionUiModel(
                    key = "rest",
                    title = "휴식 사진",
                    description = "휴식 중 사진을 1~2장 업로드",
                    iconPainter = androidx.compose.ui.res.painterResource(R.drawable.record_rest_photo),
                ),
            ),
            selectedKey = "temperature",
            onOptionSelected = {},
            confirmTitle = "선택 완료",
            onConfirm = {},
        )
    }
}
