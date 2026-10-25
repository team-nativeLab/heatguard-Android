package com.nativelap.heartguard.navigation

import androidx.lifecycle.ViewModel
import com.nativelap.heartguard.core.session.AppLinkTokenStore
import com.nativelap.heartguard.core.session.PendingAppLink
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class AppLinkTokenViewModel
    @Inject
    constructor(
        private val appLinkTokenStore: AppLinkTokenStore,
    ) : ViewModel() {
        val pendingLink = appLinkTokenStore.pendingLink

        fun accept(link: String): Boolean = appLinkTokenStore.accept(link.trim())

        fun consume(link: PendingAppLink) {
            appLinkTokenStore.consume(link)
        }
    }
