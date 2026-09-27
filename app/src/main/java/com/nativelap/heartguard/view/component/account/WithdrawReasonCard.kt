package com.nativelap.heartguard.view.component.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.account.model.WithdrawReason
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.viewmodel.account.labelResId

/** 탈퇴 사유를 하나만 고르는 라디오 목록 카드다. 선택은 필수가 아니어서 제목 옆에 "(선택)"을 표시한다.
 * 행 전체가 선택 영역이며, 이미 고른 사유를 다시 누르면 선택을 해제한다. */
@Composable
fun WithdrawReasonCard(
    selectedReason: WithdrawReason?,
    onReasonSelect: (WithdrawReason?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.NoticeCard),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .padding(
                    start = HeartGuardSpacing.Section,
                    top = HeartGuardSpacing.Section,
                    end = HeartGuardSpacing.Section,
                    bottom = HeartGuardSpacing.Compact,
                )
                .selectableGroup(),
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
            ) {
                Text(
                    text = stringResource(R.string.withdraw_reason_title),
                    color = MaterialTheme.extraColors.strongText,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = HeartGuardFontSize.CardTitle,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    text = stringResource(R.string.withdraw_reason_optional),
                    color = MaterialTheme.extraColors.secondaryText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = HeartGuardFontSize.SmallLabel,
                    ),
                )
            }

            WithdrawReason.entries.forEach { withdrawReason ->
                val isSelected = withdrawReason == selectedReason

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = {
                                val nextReason = if (isSelected) {
                                    null
                                } else {
                                    withdrawReason
                                }
                                onReasonSelect(nextReason)
                            },
                        )
                        .heightIn(min = HeartGuardComponentSize.TouchTarget),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = MaterialTheme.extraColors.disabledContent,
                        ),
                    )
                    Text(
                        text = stringResource(withdrawReason.labelResId()),
                        modifier = Modifier.padding(start = HeartGuardSpacing.Compact),
                        color = MaterialTheme.extraColors.strongText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6F9FC, widthDp = 354)
@Composable
private fun WithdrawReasonCardPreview() {
    HeartGuardTheme {
        WithdrawReasonCard(
            selectedReason = WithdrawReason.FIELD_WORK_ENDED,
            onReasonSelect = {},
        )
    }
}
