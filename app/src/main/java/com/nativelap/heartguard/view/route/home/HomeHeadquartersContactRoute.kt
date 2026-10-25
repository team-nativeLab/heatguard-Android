package com.nativelap.heartguard.view.route.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.view.screen.home.HomeHeadquartersContactScreen

@Composable
internal fun HomeHeadquartersContactRoute(onCallClick: () -> Unit) {
    HomeHeadquartersContactScreen(
        phoneNumber = stringResource(R.string.home_emergency_phone_number),
        onCallClick = onCallClick,
    )
}
