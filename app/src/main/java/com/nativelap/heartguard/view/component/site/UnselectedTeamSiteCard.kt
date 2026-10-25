package com.nativelap.heartguard.view.component.site

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 선택되지 않은 현장 행을 표시한다. 현장 전환 API가 연결되기 전에는 상호작용하지 않는다. */
@Composable
fun UnselectedTeamSiteCard(
    siteName: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .height(UNSELECTED_TEAM_SITE_CARD_HEIGHT),
        shape = RoundedCornerShape(UNSELECTED_TEAM_SITE_CARD_RADIUS),
        color = MaterialTheme.extraColors.disabledInputBackground,
        border =
            BorderStroke(
                UNSELECTED_TEAM_SITE_CARD_BORDER_WIDTH,
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = UNSELECTED_TEAM_SITE_BORDER_ALPHA),
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = UNSELECTED_TEAM_SITE_CONTENT_START,
                        end = UNSELECTED_TEAM_SITE_CONTENT_END,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = siteName,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.extraColors.homeMutedText,
                style =
                    MaterialTheme.typography.titleSmall.copy(
                        fontSize = UNSELECTED_TEAM_SITE_TITLE_SIZE,
                        lineHeight = UNSELECTED_TEAM_SITE_TITLE_LINE_HEIGHT,
                        fontWeight = FontWeight.Bold,
                    ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.team_site_selection_unselected_badge),
                color = MaterialTheme.extraColors.homeMutedText,
                style =
                    MaterialTheme.typography.labelMedium.copy(
                        fontSize = UNSELECTED_TEAM_SITE_BADGE_SIZE,
                        lineHeight = UNSELECTED_TEAM_SITE_BADGE_LINE_HEIGHT,
                        fontWeight = FontWeight.Bold,
                    ),
                maxLines = 1,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 140)
@Composable
private fun UnselectedTeamSiteCardPreview() {
    HeartGuardTheme {
        UnselectedTeamSiteCard(
            siteName = "이음산업건설 현장",
            modifier = Modifier.padding(24.dp),
        )
    }
}

private val UNSELECTED_TEAM_SITE_CARD_HEIGHT = 92.dp
private val UNSELECTED_TEAM_SITE_CARD_RADIUS = 14.dp
private val UNSELECTED_TEAM_SITE_CARD_BORDER_WIDTH = HeartGuardSpacing.Hairline
private val UNSELECTED_TEAM_SITE_CONTENT_START = 36.dp
private val UNSELECTED_TEAM_SITE_CONTENT_END = 56.dp
private val UNSELECTED_TEAM_SITE_TITLE_SIZE = 16.sp
private val UNSELECTED_TEAM_SITE_TITLE_LINE_HEIGHT = 26.sp
private val UNSELECTED_TEAM_SITE_BADGE_SIZE = 12.sp
private val UNSELECTED_TEAM_SITE_BADGE_LINE_HEIGHT = 18.sp
private const val UNSELECTED_TEAM_SITE_BORDER_ALPHA = 0.68f
