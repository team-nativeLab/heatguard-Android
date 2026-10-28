package com.nativelap.heartguard.view.route.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nativelap.heartguard.core.util.isValidPhoneNumber
import com.nativelap.heartguard.view.component.emergency.formatKoreanPhoneNumberForDisplay
import com.nativelap.heartguard.view.component.valueOrEmptyText
import com.nativelap.heartguard.view.screen.home.HomeHeadquartersContactScreen
import com.nativelap.heartguard.viewmodel.home.HomeViewModel

@Composable
internal fun HomeHeadquartersContactRoute(
    homeViewModel: HomeViewModel,
    onCallClick: () -> Unit,
) {
    val siteStatus by homeViewModel.siteStatus.collectAsStateWithLifecycle()
    val managerPhoneNumber = siteStatus.managerPhoneNumber
    val validManagerPhoneNumber = managerPhoneNumber?.takeIf(String::isValidPhoneNumber)

    HomeHeadquartersContactScreen(
        phoneNumber = valueOrEmptyText(validManagerPhoneNumber?.let(::formatKoreanPhoneNumberForDisplay)),
        isCallEnabled = validManagerPhoneNumber != null,
        onCallClick = onCallClick,
    )
}
