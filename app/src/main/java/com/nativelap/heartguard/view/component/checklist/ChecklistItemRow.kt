package com.nativelap.heartguard.view.component.checklist

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.control.HeartGuardCheckbox

@Composable
internal fun ChecklistItemRow(
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = HeartGuardComponentSize.ChecklistItemRowHeight)
                .toggleable(
                    value = isChecked,
                    role = Role.Checkbox,
                    onValueChange = onCheckedChange,
                ).padding(horizontal = HeartGuardSpacing.ChecklistItemHorizontal),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeartGuardCheckbox(
            isChecked = isChecked,
            onCheckedChange = {},
            contentDescription = title,
            isInteractive = false,
            isCircular = true,
        )

        Text(
            text = title,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = HeartGuardSpacing.ChecklistItemTextGap),
            color =
                if (isChecked) {
                    MaterialTheme.extraColors.checklistCompletedText
                } else {
                    MaterialTheme.extraColors.strongText
                },
            style =
                MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Normal,
                ),
        )
    }
}
