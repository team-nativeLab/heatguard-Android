package com.nativelap.heartguard.view.component.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 메뉴 드로어의 한 줄 항목이다. [showsChevron]이 true면 다른 화면으로 이동하는 항목임을 오른쪽 "›"로 표시한다. */
@Composable
fun MenuDrawerItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showsChevron: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HeartGuardComponentSize.TouchTarget)
            .clickable(
                role = Role.Button,
                onClick = onClick,
            )
            .padding(vertical = HeartGuardSpacing.MenuItemVertical),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.extraColors.strongText,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = HeartGuardFontSize.MenuItem,
                fontWeight = FontWeight.Medium,
            ),
        )

        if (showsChevron) {
            Text(
                text = stringResource(R.string.common_chevron_right),
                modifier = Modifier.clearAndSetSemantics {},
                color = MaterialTheme.extraColors.tertiaryText,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 252)
@Composable
private fun MenuDrawerItemPreview() {
    HeartGuardTheme {
        MenuDrawerItem(
            title = "내 정보 수정",
            onClick = {},
        )
    }
}
