package com.nativelap.heartguard.view.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.view.component.HeartGuardSheetSurface
import com.nativelap.heartguard.view.component.emergency.EmergencyContactCard

@Composable
internal fun HomeHeadquartersContactScreen(
    phoneNumber: String,
    onCallClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HeartGuardSheetSurface(modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = HeartGuardSpacing.RecordPhotoCardHorizontal,
                        vertical = HeartGuardSpacing.LargeSection,
                    ),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Section),
        ) {
            Text(
                text = stringResource(R.string.home_emergency_call),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )

            EmergencyContactCard(
                contactTitle = stringResource(R.string.emergency_contact_title),
                contactName = stringResource(R.string.home_headquarters_contact_name),
                phoneNumber = phoneNumber,
                onCallClick = onCallClick,
            )
        }
    }
}
