package com.nativelap.heartguard.view.component.control

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardBorderWidth
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import androidx.compose.material3.MaterialTheme

/** Figma의 24dp 체크박스와 48dp 터치 영역을 함께 제공하는 선택 컨트롤이다. */
@Composable
fun HeartGuardCheckbox(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    val checkboxFillColor = when {
        !isEnabled -> MaterialTheme.extraColors.disabledContent
        isChecked -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surface
    }
    val checkboxBorderColor = when {
        !isEnabled -> MaterialTheme.extraColors.disabledContent
        isChecked -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val checkMarkColor = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier
            .size(HeartGuardComponentSize.TouchTarget)
            .semantics { this.contentDescription = contentDescription }
            .toggleable(
                value = isChecked,
                enabled = isEnabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(HeartGuardIconSize.Checkbox)) {
            val borderWidth = HeartGuardBorderWidth.Checkbox.toPx()
            val cornerRadius = HeartGuardRadius.Checkbox.toPx()

            drawRoundRect(
                color = checkboxFillColor,
                cornerRadius = CornerRadius(cornerRadius),
            )
            drawRoundRect(
                color = checkboxBorderColor,
                cornerRadius = CornerRadius(cornerRadius),
                style = Stroke(width = borderWidth),
            )

            if (isChecked) {
                drawLine(
                    color = checkMarkColor,
                    start = androidx.compose.ui.geometry.Offset(size.width * 0.22f, size.height * 0.52f),
                    end = androidx.compose.ui.geometry.Offset(size.width * 0.44f, size.height * 0.73f),
                    strokeWidth = borderWidth,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = checkMarkColor,
                    start = androidx.compose.ui.geometry.Offset(size.width * 0.44f, size.height * 0.73f),
                    end = androidx.compose.ui.geometry.Offset(size.width * 0.79f, size.height * 0.31f),
                    strokeWidth = borderWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HeartGuardCheckboxPreview() {
    HeartGuardTheme {
        HeartGuardCheckbox(
            isChecked = true,
            onCheckedChange = {},
            contentDescription = "선택됨",
        )
    }
}
