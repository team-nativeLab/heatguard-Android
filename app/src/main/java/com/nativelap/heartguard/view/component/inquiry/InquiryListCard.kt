package com.nativelap.heartguard.view.component.inquiry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.inquiry.model.InquiryStatus
import com.nativelap.heartguard.domain.inquiry.model.InquirySummary
import com.nativelap.heartguard.ui.theme.HeartGuardBorderWidth
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.valueOrEmptyText
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 내 문의 목록 카드다(Figma 25). 문의마다 상태 배지·제목·등록일을 보여준다.
 * 문의 상세 화면은 이번 범위가 아니어서 항목은 눌리지 않고 화살표도 두지 않는다. 등록일은 한국 시간으로 표시한다. */
@Composable
fun InquiryListCard(
    inquiries: List<InquirySummary>,
    modifier: Modifier = Modifier,
) {
    val dateFormatter =
        DateTimeFormatter.ofPattern(
            stringResource(R.string.inquiry_date_format),
            Locale.KOREAN,
        )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.Card),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            inquiries.forEachIndexed { inquiryIndex, inquirySummary ->
                if (inquiryIndex > 0) {
                    HorizontalDivider(
                        thickness = HeartGuardBorderWidth.Divider,
                        color = MaterialTheme.extraColors.cardBorder,
                    )
                }
                Column(
                    modifier =
                        Modifier.padding(
                            horizontal = HeartGuardSpacing.Card,
                            vertical = HeartGuardSpacing.Item,
                        ),
                    verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
                    ) {
                        InquiryStatusBadge(inquiryStatus = inquirySummary.status)
                        Text(
                            text = valueOrEmptyText(inquirySummary.title),
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.extraColors.strongText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text =
                            inquirySummary.createdAt
                                ?.atZoneSameInstant(KOREA_ZONE)
                                ?.format(dateFormatter)
                                ?: stringResource(R.string.common_empty_value),
                        color = MaterialTheme.extraColors.secondaryText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

// 서버는 UTC로 등록 시각을 준다. 날짜 경계가 어긋나지 않도록 명세 기준 시간대(Asia/Seoul)로 바꿔 표시한다.
private val KOREA_ZONE: ZoneId = ZoneId.of("Asia/Seoul")

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun InquiryListCardPreview() {
    HeartGuardTheme {
        InquiryListCard(
            inquiries =
                listOf(
                    InquirySummary(
                        inquiryId = "inq_01",
                        title = "앱에서 사진 업로드가 안 돼요",
                        status = InquiryStatus.ANSWERED,
                        replyCount = 1,
                        createdAt = OffsetDateTime.parse("2026-09-25T01:00:00Z"),
                    ),
                ),
        )
    }
}
