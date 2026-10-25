package com.nativelap.heartguard.view.component.photo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardBorderWidth
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** Figma 06/14/15의 "현장 사진"·"다시하기" 영역처럼 굵은 라벨과 카메라 아이콘·두 줄 안내가 있는 촬영 진입 카드다.
 * [isEnabled]가 false면(사진 2장을 이미 골랐거나 온도계 미설치로 사진이 필요 없을 때) 회색으로 바뀌고 누를 수 없다.
 * [labelHorizontalOffset]은 Figma처럼 라벨을 카드보다 안쪽에서 시작할 때 쓴다. */
@Composable
fun PhotoCaptureRow(
    label: String?,
    instructionText: String,
    cameraPainter: Painter,
    cameraContentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    labelHorizontalOffset: Dp = 0.dp,
) {
    val contentColor =
        if (isEnabled) {
            MaterialTheme.extraColors.homeMutedText
        } else {
            MaterialTheme.extraColors.disabledContent
        }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
    ) {
        label?.let { labelText ->
            Text(
                text = labelText,
                modifier = Modifier.padding(start = labelHorizontalOffset),
                color = MaterialTheme.colorScheme.onSurface,
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontSize = HeartGuardFontSize.PageTitle,
                        fontWeight = FontWeight.Bold,
                    ),
            )
        }

        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = HeartGuardComponentSize.FieldPhotoCaptureRowHeight)
                    .clickable(
                        enabled = isEnabled,
                        role = Role.Button,
                        onClick = onClick,
                    ),
            shape = RoundedCornerShape(HeartGuardRadius.PrimaryAction),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(HeartGuardBorderWidth.Divider, MaterialTheme.extraColors.cardBorder),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = HeartGuardSpacing.Section,
                            vertical = HeartGuardSpacing.Item,
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = cameraPainter,
                    contentDescription = cameraContentDescription,
                    modifier = Modifier.size(HeartGuardIconSize.CameraAction),
                    colorFilter =
                        ColorFilter.tint(
                            if (isEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.extraColors.disabledContent
                            },
                        ),
                )
                Text(
                    text = instructionText,
                    modifier = Modifier.weight(1f),
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontSize = HeartGuardFontSize.SmallLabel,
                            fontWeight = FontWeight.SemiBold,
                        ),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun PhotoCaptureRowPreview() {
    HeartGuardTheme {
        PhotoCaptureRow(
            label = "다시하기",
            instructionText = "사진 촬영 또는\n앨범에서 선택",
            cameraPainter = painterResource(R.drawable.record_camera),
            cameraContentDescription = "사진 촬영",
            onClick = {},
        )
    }
}
