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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme

/** 현장 사진 화면의 "다시하기" 영역처럼 상단 라벨과 한 줄짜리 촬영 안내 박스를 함께 보여주는 컴포넌트다. */
@Composable
fun PhotoCaptureRow(
    label: String,
    instructionText: String,
    cameraPainter: Painter,
    cameraContentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
        )

        Surface(
            modifier = Modifier
                .padding(top = HeartGuardSpacing.Compact)
                .fillMaxWidth()
                .heightIn(min = HeartGuardComponentSize.FieldPhotoCaptureRowHeight)
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
                ),
            shape = RoundedCornerShape(HeartGuardRadius.Card),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(HeartGuardSpacing.Hairline, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HeartGuardSpacing.Section),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
            ) {
                Image(
                    painter = cameraPainter,
                    contentDescription = cameraContentDescription,
                    modifier = Modifier.size(HeartGuardIconSize.Small),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                )
                Text(
                    text = instructionText,
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = HeartGuardComponentSize.PhotoCaptureTextMaxWidth),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
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
            instructionText = "사진 촬영 또는 앨범에서 선택",
            cameraPainter = androidx.compose.ui.res.painterResource(R.drawable.record_camera),
            cameraContentDescription = "사진 촬영",
            onClick = {},
        )
    }
}
