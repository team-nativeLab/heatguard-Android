package com.nativelap.heartguard.view.component.inquiry

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.inquiry.model.InquiryStatus
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 문의 처리 상태 배지다(Figma 25). 답변 완료는 초록, 답변 대기는 주황, 종료·알 수 없음은 회색이다. */
@Composable
fun InquiryStatusBadge(
    inquiryStatus: InquiryStatus,
    modifier: Modifier = Modifier,
) {
    val (containerColor, labelColor) =
        when (inquiryStatus) {
            InquiryStatus.ANSWERED -> {
                MaterialTheme.extraColors.successContainer to MaterialTheme.extraColors.success
            }

            InquiryStatus.OPEN -> {
                MaterialTheme.extraColors.warningContainer to
                    MaterialTheme.extraColors.onWarningContainer
            }

            InquiryStatus.CLOSED,
            InquiryStatus.UNKNOWN,
            -> {
                MaterialTheme.extraColors.photoContainer to MaterialTheme.extraColors.secondaryText
            }
        }
    val statusText =
        when (inquiryStatus) {
            InquiryStatus.OPEN -> stringResource(R.string.inquiry_status_open)
            InquiryStatus.ANSWERED -> stringResource(R.string.inquiry_status_answered)
            InquiryStatus.CLOSED -> stringResource(R.string.inquiry_status_closed)
            InquiryStatus.UNKNOWN -> stringResource(R.string.common_empty_value)
        }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(HeartGuardRadius.Pill),
        color = containerColor,
        contentColor = labelColor,
    ) {
        Text(
            text = statusText,
            modifier =
                Modifier.padding(
                    horizontal = HeartGuardSpacing.Compact,
                    vertical = HeartGuardSpacing.Hairline,
                ),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        )
    }
}

@Preview
@Composable
private fun InquiryStatusBadgePreview() {
    HeartGuardTheme {
        InquiryStatusBadge(inquiryStatus = InquiryStatus.ANSWERED)
    }
}
