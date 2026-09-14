package com.nativelap.heartguard.view.component.photo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardBorderWidth
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 저장 전 온도계 데이터가 없는 상태를 사진 화면 안에서 안내한다. */
@Composable
fun TemperatureDataErrorCard(
    message: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.LargeCard),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.extraColors.photoContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = HeartGuardComponentSize.PhotoSelectionHeight)
                .padding(HeartGuardSpacing.Section),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
        ) {
            Surface(
                modifier = Modifier.size(HeartGuardIconSize.WeatherStatus),
                shape = CircleShape,
                color = androidx.compose.ui.graphics.Color.Transparent,
                border = BorderStroke(
                    HeartGuardBorderWidth.Checkbox,
                    MaterialTheme.colorScheme.primary,
                ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                text = message,
                modifier = Modifier.widthIn(max = HeartGuardComponentSize.TemperatureErrorMessageMaxWidth),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun TemperatureDataErrorCardPreview() {
    HeartGuardTheme {
        TemperatureDataErrorCard(
            message = "온도계가 아직 저장이 안되었어요",
        )
    }
}
